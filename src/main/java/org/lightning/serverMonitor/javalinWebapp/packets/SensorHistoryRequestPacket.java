package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.javalinWebapp.PacketContext;
import org.lightning.serverMonitor.monitor.SensorDatabaseWriter;

public record SensorHistoryRequestPacket
        (String sensor, long startDate, long endDate) implements WsPacket {

    public void handle(PacketContext ctx) {
        SensorDatabaseWriter.HistoryEntry[] sensorDataRange = SensorDatabaseWriter.getSensorDataRange(sensor, startDate, endDate);
        System.out.println("Sensor data range: " + sensorDataRange.length);
        ctx.send(new SensorHistoryPacket(sensorDataRange));
    }
}