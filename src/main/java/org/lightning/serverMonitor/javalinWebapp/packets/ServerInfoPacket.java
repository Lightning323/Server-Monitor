package org.lightning.serverMonitor.javalinWebapp.packets;

import io.javalin.websocket.WsContext;

public record ServerInfoPacket(
        String appVersion, String serverName) implements WsPacket{
    @Override
    public void handle(WsContext ctx) {
    }
}
