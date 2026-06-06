package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.javalinWebapp.PacketContext;
import org.lightning.serverMonitor.sensorMonitoring.SensorDatabase;

import java.util.List;

public record SensorHistoryRequestPacket
        (String sensor, long startDate, long endDate) implements WsPacket {

    public void handle(PacketContext ctx) {
        List<SensorDatabase.HistoryEntry> data = SensorDatabase.getSensorDataRange(sensor, startDate, endDate, 2000);
        System.out.println("Sensor data range for " + sensor + ": " + data.size());
        if (data.size() == 0) {
            ctx.send(new SensorHistoryPacket(sensor, 0, data.toArray(new SensorDatabase.HistoryEntry[0]), true));
        } else {
            final int CHUNK_SIZE = 1000;
            for (int i = 0; i < data.size(); i += CHUNK_SIZE) {
                if (!ctx.getContext().session.isOpen()) break;
                int end = Math.min(data.size(), i + CHUNK_SIZE);
                List<SensorDatabase.HistoryEntry> chunk = data.subList(i, end);
                boolean isLast = (end == data.size());
                ctx.send(new SensorHistoryPacket(sensor, i, chunk.toArray(new SensorDatabase.HistoryEntry[0]), isLast));
            }
        }
        //When we are done, tell the client the time over temp
        ctx.send(new TimeOverTempPacket(sensor, data));
    }
}