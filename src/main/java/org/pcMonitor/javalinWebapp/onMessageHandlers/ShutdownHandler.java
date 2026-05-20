package org.pcMonitor.javalinWebapp.onMessageHandlers;

import io.javalin.websocket.WsMessageContext;
import org.pcMonitor.javalinWebapp.JavalinWebApp;
import org.pcMonitor.javalinWebapp.OnMessageHandlers;
import org.pcMonitor.platform.Platform;

public class ShutdownHandler extends OnMessageHandlers {

    public ShutdownHandler(JavalinWebApp app) {
        super("shutdown", app);
    }

    public void onMessage(WsMessageContext ctx, String message) {
        if (message.isBlank() || message.equals("shutdown")) {
            Platform.SINGLETON.shutdown("webapp");
        } else {
            Platform.SINGLETON.suspend("webapp");
        }
    }
}
