package org.lightning.serverMonitor;

import io.javalin.websocket.WsConnectContext;
import org.lightning.serverMonitor.javalinWebapp.packets.ServerInfoPacket;
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
    public static Settings settings = Settings.load();
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
                System.out.println("Client connected: ");
                sendPacket(ctx, new ServerInfoPacket(APP_VERSION, Main.settings.SERVER_NAME));
            }
        };
        webApp.registerPacket(ServerInfoPacket.class);

        webApp.start(3000);
        System.out.println("Webapp started on port 3000");

//        //Start temp monitor
//        tempMonitor.start();
//
//        //Start taskbar tray
//        taskbarTray.start();
    }
}