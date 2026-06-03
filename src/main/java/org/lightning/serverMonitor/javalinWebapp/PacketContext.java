package org.lightning.serverMonitor.javalinWebapp;

import io.javalin.websocket.WsContext;
import org.lightning.serverMonitor.javalinWebapp.packets.WsPacket;

public class PacketContext {
    private final WsContext context;
    private final JavalinWebApp app;

    public PacketContext(WsContext context, JavalinWebApp app) {
        this.context = context;
        this.app = app;
    }

    public WsContext getContext() {
        return context;
    }

    public JavalinWebApp getWebapp() {
        return app;
    }

    public void send(WsPacket packet) {
        app.sendPacket(context, packet);
    }
}