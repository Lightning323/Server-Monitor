package org.lightning.serverMonitor.javalinWebapp;

import io.javalin.websocket.WsMessageContext;

public abstract class OnMessageHandlers {

    public final String prefix;
    public final JavalinWebApp app;

    public OnMessageHandlers(String prefix, JavalinWebApp app) {
        this.prefix = prefix;
        this.app = app;
    }

    public abstract void onMessage(WsMessageContext ctx, String message);
}
