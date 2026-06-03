package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.javalinWebapp.PacketContext;

public record ServerInfoPacket(
        String appVersion, String serverName) implements WsPacket{
    @Override
    public void handle(PacketContext ctx) {
    }
}
