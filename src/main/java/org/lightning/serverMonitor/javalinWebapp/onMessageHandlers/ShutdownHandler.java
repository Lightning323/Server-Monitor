package org.lightning.serverMonitor.javalinWebapp.onMessageHandlers;

import io.javalin.websocket.WsMessageContext;
import org.lightning.serverMonitor.javalinWebapp.JavalinWebApp;
import org.lightning.serverMonitor.javalinWebapp.OnMessageHandlers;
import org.lightning.serverMonitor.platform.Platform;

public class ShutdownHandler extends OnMessageHandlers {

    public ShutdownHandler(JavalinWebApp app) {
        super("shutdown", app);
    }

    public void onMessage(WsMessageContext ctx, String message) {
//        if (message.isBlank() || message.equals("shutdown")) {
//            Platform.SINGLETON.shutdown("webapp");
//        } else {
//            Platform.SINGLETON.suspend("webapp");
//        }
    }
}
