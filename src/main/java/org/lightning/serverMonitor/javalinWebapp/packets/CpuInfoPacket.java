package org.lightning.serverMonitor.javalinWebapp.packets;

import io.javalin.websocket.WsContext;

public record CpuInfoPacket(String cpuInfo) implements WsPacket {
}
