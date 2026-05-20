package org.lightning.serverMonitor.applets.temprature;

import org.lightning.serverMonitor.Main;
import org.lightning.serverMonitor.javalinWebapp.JavalinWebApp;
import org.lightning.serverMonitor.platform.Platform;
import org.lightning.serverMonitor.utils.MiscUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CPUTempMonitor {

    private static long msOver60C, msOver65C, msOver70C, msOver75C, msOver80C, msOver85C, msOver90C;

    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    Platform stats;
    JavalinWebApp webApp;
    long index = 0;

    public CPUTempMonitor(Platform stats, JavalinWebApp webApp) {
        this.stats = stats;
        this.webApp = webApp;
    }

    public String getTimeOverTemp() {
        return new StringBuilder()
                .append("> 60C: ").append(MiscUtils.convertMsToHMS(msOver60C)).append("\n")
                .append("> 65C: ").append(MiscUtils.convertMsToHMS(msOver65C)).append("\n")
                .append("> 70C: ").append(MiscUtils.convertMsToHMS(msOver70C)).append("\n")
                .append("> 75C: ").append(MiscUtils.convertMsToHMS(msOver75C)).append("\n")
                .append("> 80C: ").append(MiscUtils.convertMsToHMS(msOver80C)).append("\n")
                .append("> 85C: ").append(MiscUtils.convertMsToHMS(msOver85C)).append("\n")
                .append("> 90C: ").append(MiscUtils.convertMsToHMS(msOver90C)).toString();
    }


    public void start() {
        //A shceduler that runs every UPDATE_MS ms
        scheduler.scheduleAtFixedRate(() -> {
            try {
                //Get immediate statistics
                double cpuLoad = stats.getImmediateCPULoad();
                double cpuTemp = stats.getCPUTempCelsius();
                webApp.addDataPoint(cpuTemp, cpuLoad);
//                System.out.println("Temp: "+cpuTemp+"; Load: "+cpuLoad);

                if (cpuTemp > 60) {
                    msOver60C += Main.settings.SENSORS_UPDATE_MS;
                }
                if (cpuTemp > 65) {
                    msOver65C += Main.settings.SENSORS_UPDATE_MS;
                }
                if (cpuTemp > 70) {
                    msOver70C += Main.settings.SENSORS_UPDATE_MS;
                }
                if (cpuTemp > 75) {
                    msOver75C += Main.settings.SENSORS_UPDATE_MS;
                }
                if (cpuTemp > 80) {
                    msOver80C += Main.settings.SENSORS_UPDATE_MS;
                }
                if (cpuTemp > 85) {
                    msOver85C += Main.settings.SENSORS_UPDATE_MS;
                }
                if (cpuTemp > 90) {
                    msOver90C += Main.settings.SENSORS_UPDATE_MS;
                }


                TempratureProtectionApplet.checkTemp(cpuTemp);

                index++;
            } catch (Exception e) {
                if (Main.settings.PROTECTION_SHUTDOWN_ON_TEMP_ERROR) {
                    Platform.SINGLETON.shutdown("Error with CPU temp monitor: " + e.getMessage());
                } else {
                    Main.LOGGER.error("Error with CPU temp monitor",e);
                    TempratureProtectionApplet.downclock();
                }
            }
        }, 0, Main.settings.SENSORS_UPDATE_MS, TimeUnit.MILLISECONDS);
    }



    public void shutdown() {
        scheduler.shutdownNow();
    }
}
