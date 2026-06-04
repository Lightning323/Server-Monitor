package org.lightning.serverMonitor;

import io.javalin.websocket.WsConnectContext;
import org.lightning.serverMonitor.config.Config;
import org.lightning.serverMonitor.javalinWebapp.packets.*;
import org.lightning.serverMonitor.monitor.SensorMonitor;
import org.lightning.serverMonitor.javalinWebapp.JavalinWebApp;
import org.lightning.serverMonitor.platform.Platform;
import org.lightning.serverMonitor.utils.ExtendedLogger;
import org.lightning.serverMonitor.utils.MiscUtils;


public class Main {
    public static final String APP_VERSION = "1.0.0";
    public static final boolean DEV_ENV = System.getProperty("env", "dev").equals("dev");
    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ExtendedLogger LOGGER = new ExtendedLogger();
    public static TaskbarTray taskbarTray = new TaskbarTray();
    public static Config config = Config.load();
    public static JavalinWebApp webApp;
    public static SensorMonitor tempMonitor = new SensorMonitor(Platform.SINGLETON);


    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        System.out.println("Version: " + APP_VERSION + "; Dev environment: " + DEV_ENV);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.notification("shutting down; Runtime: " + MiscUtils.convertMsToHMS(System.currentTimeMillis() - startTime));
        }));

        //Start webapp
        webApp = new JavalinWebApp() {
            public void onConnect(WsConnectContext ctx) {
                super.onConnect(ctx);
                sendPacket(ctx, new ServerInfoPacket(APP_VERSION, Main.config.SERVER_NAME));
                sendPacket(ctx, Platform.SINGLETON.getSystemInfo());
                sendPacket(ctx, new SensorAliasesPacket(config.SENSOR_ALIASES));
//                sendPacket(ctx, new SensorsSelectedPacket(config.SELECTED_SENSORS));
            }
        };
        webApp.registerPacket(ServerInfoPacket.class);
        webApp.registerPacket(SensorDumpPacket.class);
        webApp.registerPacket(SystemInfo.class);
        webApp.registerPacket(SensorHistoryPacket.class);
        webApp.registerPacket(SensorHistoryRequestPacket.class);
        webApp.registerPacket(SensorAliasesPacket.class);
//        webApp.registerPacket(SensorsSelectedPacket.class);
        webApp.start(config.WEBAPP_PORT);

        //Start temp monitor
        tempMonitor.start();
        tempMonitor.sensorCallback = (sensorData) -> {
            webApp.broadcastPacket(new SensorDumpPacket(sensorData));
        };

        //Start taskbar tray
        taskbarTray.start();
    }
}