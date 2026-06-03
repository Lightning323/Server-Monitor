package org.lightning.serverMonitor.monitor;

import org.lightning.serverMonitor.Main;
import org.lightning.serverMonitor.platform.Platform;
import org.lightning.serverMonitor.sensorMonitoring.SensorDump;
import org.lightning.serverMonitor.sensorMonitoring.SensorProperty;
import org.lightning.serverMonitor.sensorMonitoring.TimeOverTemp;
import org.lightning.serverMonitor.utils.SensorDatabaseWriter;
import org.lightning.serverMonitor.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SensorMonitor {

    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    Platform platform;
    final List<SensorDump> history;
    private static final int DATABASE_RECORD_WRITE_INTERVAL = 1;
    public final TimeOverTemp cpuTimeOverTemp;
    public Consumer<Double> cpuTempCallback;

    public SensorMonitor(Platform platform) {
        this.platform = platform;
        history = new ArrayList<>();
        cpuTimeOverTemp = new TimeOverTemp(new ArrayList<>(List.of(45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100)));
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                SensorDump sensorData = SensorDump.read();
                history.add(sensorData);
//                System.out.println(sensorData);

                //Write to log
                if (history.size() > DATABASE_RECORD_WRITE_INTERVAL) {
                    SensorDatabaseWriter.saveMultipleDumps(history);
                    history.clear();
                }

                //Process CPU temp
                SensorProperty cpuTempProp = sensorData.getSensor(Main.settings.LINUX_CPU_TEMP_SENSOR_NAME, Main.settings.LINUX_CPU_TEMP_KEY);
                double cpuTemp = -1;
                if (cpuTempProp != null) cpuTemp = StringUtils.extractDouble(cpuTempProp.value);
                if (cpuTempCallback != null) cpuTempCallback.accept(cpuTemp);
                cpuTimeOverTemp.accumulate(cpuTemp, Main.settings.SENSORS_UPDATE_MS);
            } catch (Throwable e) {
                Main.LOGGER.error("Error with CPU temp monitor", e);
            }
        }, 0, Main.settings.SENSORS_UPDATE_MS, TimeUnit.MILLISECONDS);
    }


    public void shutdown() {
        scheduler.shutdownNow();
    }

    public static void main(String[] args) {
        SensorMonitor sensorMonitor = new SensorMonitor(Platform.SINGLETON);
        sensorMonitor.start();
    }
}
