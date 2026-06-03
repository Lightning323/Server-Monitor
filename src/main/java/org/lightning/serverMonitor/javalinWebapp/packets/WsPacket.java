package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.javalinWebapp.PacketContext;

public interface WsPacket {

    default public void handle(PacketContext ctx) {
    }
}
