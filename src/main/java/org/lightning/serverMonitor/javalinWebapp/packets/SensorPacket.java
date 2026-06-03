package org.lightning.serverMonitor.javalinWebapp.packets;

import io.javalin.websocket.WsContext;

public record SensorPacket(String sensorId, double temperature, long timestamp) implements WsPacket {

    @Override
    public void handle(WsContext ctx) {

    }
}