package org.lightning.serverMonitor.javalinWebapp.onMessageHandlers;

import io.javalin.websocket.WsMessageContext;
import org.lightning.serverMonitor.javalinWebapp.JavalinWebApp;
import org.lightning.serverMonitor.javalinWebapp.OnMessageHandlers;

import static org.lightning.serverMonitor.javalinWebapp.JavalinWebApp.DELIMITER;

public class LoadHistoryHandler extends OnMessageHandlers {
    public LoadHistoryHandler(JavalinWebApp app) {
        super("load-history",app);
    }

    public void onMessage(WsMessageContext ctx, String message) {
        String filename = null;
        if (message.split(DELIMITER).length > 1) filename = message.split(DELIMITER)[1];
        System.out.println("Loading history: " + filename);
        if (filename != null) app.sendHistory(ctx, filename);
        else app.sendHistory(ctx);
    }
}
