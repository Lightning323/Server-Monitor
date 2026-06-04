package org.lightning.serverMonitor;

import io.javalin.websocket.WsConnectContext;
import org.lightning.serverMonitor.config.Config;
import org.lightning.serverMonitor.javalinWebapp.packets.*;
import org.lightning.serverMonitor.sensorMonitoring.SensorDatabase;
import org.lightning.serverMonitor.javalinWebapp.JavalinWebApp;
import org.lightning.serverMonitor.platform.Platform;
import org.lightning.serverMonitor.sensorMonitoring.SensorDump;
import org.lightning.serverMonitor.utils.ExtendedLogger;
import org.lightning.serverMonitor.utils.MiscUtils;
import org.lightning.serverMonitor.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Main {
    public static final String APP_VERSION = "2.1.0";
    //Add -Denv=dev VM option in run configuration
    public static final boolean DEV_ENV = System.getProperty("env") != null && System.getProperty("env").equals("dev");
    /// //////////////////////////////////////////////////////////////////////////////////////
    /// //////////////////////////////////////////////////////////////////////////////////////
    /// //////////////////////////////////////////////////////////////////////////////////////
    /// //////////////////////////////////////////////////////////////////////////////////////

    public static ExtendedLogger LOGGER = new ExtendedLogger();
    public static TaskbarTray taskbarTray = new TaskbarTray();
    public static Config config = Config.load();
    public static JavalinWebApp webApp;

    //Monitor
    static long lastDatabaseWrite;
    static long lastMetricsWrite;
    static long lastTempNotification;
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final List<SensorDump> cachedHistory = new ArrayList<>();
    private static final List<SensorDump> lastNSamples = new ArrayList<>();
    private static final int lastNSamplesSize = 250;

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        LOGGER.debug("System property 'env' is: " + System.getProperty("env"));
        System.out.println("Version: " + APP_VERSION + "; Dev environment: " + DEV_ENV);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.notification("shutting down; Runtime: " + MiscUtils.convertMsToHMS(System.currentTimeMillis() - startTime));
            writeToDatabase();
        }));

        //Start webapp
        webApp = new JavalinWebApp() {
            public void onConnect(WsConnectContext ctx) {
                super.onConnect(ctx);
                sendPacket(ctx, new ServerInfoPacket(APP_VERSION, Main.config.SERVER_NAME));
                sendPacket(ctx, Platform.SINGLETON.getSystemInfo());
                sendPacket(ctx, new SensorAliasesPacket(config.SENSOR_ALIASES));
                sendPacket(ctx, new SensorDumpPacket(lastNSamples));
//                sendPacket(ctx, new SensorsSelectedPacket(config.SELECTED_SENSORS));
            }
        };
        System.out.println("Starting webapp on port " + config.WEBAPP_PORT);
        webApp.registerPacket(ServerInfoPacket.class);
        webApp.registerPacket(SensorDumpPacket.class);
        webApp.registerPacket(SystemInfo.class);
        webApp.registerPacket(SensorHistoryPacket.class);
        webApp.registerPacket(SensorHistoryRequestPacket.class);
        webApp.registerPacket(SensorAliasesPacket.class);
        webApp.start(config.WEBAPP_PORT);

        //Start taskbar tray
        taskbarTray.start();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                SensorDump sensorData = SensorDump.read();
                cachedHistory.add(sensorData);
                lastNSamples.add(sensorData);
                if (lastNSamples.size() > lastNSamplesSize) {
                    lastNSamples.remove(0);
                }
                webApp.broadcastPacket(new SensorDumpPacket(sensorData));

                //Write to log
                if (System.currentTimeMillis() - lastDatabaseWrite > Main.config.DATABASE_RECORD_WRITE_INTERVAL_MS && !cachedHistory.isEmpty()) {
                    writeToDatabase();
                }
                //Send metrics
                if (Main.config.METRICS_UPDATE_MS > 0
                        && System.currentTimeMillis() - lastMetricsWrite > Main.config.METRICS_UPDATE_MS) {
                    webApp.broadcastPacket(Platform.SINGLETON.getSystemInfo());
                    lastMetricsWrite = System.currentTimeMillis();
                }

                //Check temperature notifications
                checkTempNotifications(sensorData);

            } catch (Throwable e) {
                Main.LOGGER.error("Error with CPU temp monitor", e);
            }
        }, 0, Main.config.SENSORS_UPDATE_MS, TimeUnit.MILLISECONDS);
    }

    private static boolean tempNotificationsEnabled() {
        return Main.config.TEMP_NOTIFICATIONS != null && !Main.config.TEMP_NOTIFICATIONS.isEmpty()
                && Main.config.DISCORD_WEBHOOK_URL != null && !Main.config.DISCORD_WEBHOOK_URL.isEmpty();
    }

    private static void checkTempNotifications(SensorDump sensorData) {
        if (tempNotificationsEnabled()
                && System.currentTimeMillis() - lastTempNotification > Main.config.TEMP_NOTIFICATION_MS) {
            lastTempNotification = System.currentTimeMillis();
            sensorData.forEachSensor((sensor, value) -> {
                if (Main.config.TEMP_NOTIFICATIONS.containsKey(sensor)) {
                    double numVal = StringUtils.extractDouble(value);
                    if (numVal > Main.config.TEMP_NOTIFICATIONS.get(sensor)) {
                        String sensorAlias = config.SENSOR_ALIASES.getOrDefault(sensor, sensor);
                        LOGGER.notification(sensorAlias + " is above " + Main.config.TEMP_NOTIFICATIONS.get(sensor) + " (" + value + ")");
                    }
                }
            });
        }
    }

    private static void writeToDatabase() {
        LOGGER.debug("Writing to database " + cachedHistory.size() + " records");
        SensorDatabase.saveMultipleDumps(cachedHistory);
        cachedHistory.clear();
        lastDatabaseWrite = System.currentTimeMillis();
    }
}