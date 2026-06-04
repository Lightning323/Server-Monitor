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
    public static final boolean TEST_MODE = true;
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
        System.out.println("Version: " + APP_VERSION);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.notification("shutting down; Runtime: " + MiscUtils.convertMsToHMS(System.currentTimeMillis() - startTime));
        }));

        //Start webapp
        webApp = new JavalinWebApp() {
            public void onConnect(WsConnectContext ctx) {
                super.onConnect(ctx);
                sendPacket(ctx, new ServerInfoPacket(APP_VERSION, Main.config.SERVER_NAME));
                sendPacket(ctx, new CpuInfoPacket(Platform.SINGLETON.getCPUInfo()));
                sendPacket(ctx, new SensorAliasesPacket(config.SENSOR_ALIASES));
            }
        };
        webApp.registerPacket(ServerInfoPacket.class);
        webApp.registerPacket(SensorDumpPacket.class);
        webApp.registerPacket(CpuInfoPacket.class);
        webApp.registerPacket(SensorHistoryPacket.class);
        webApp.registerPacket(SensorHistoryRequestPacket.class);
        webApp.registerPacket(SensorAliasesPacket.class);
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