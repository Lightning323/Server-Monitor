package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.javalinWebapp.PacketContext;
import org.lightning.serverMonitor.monitor.SensorDatabaseWriter;

import java.util.Arrays;
import java.util.List;

public record SensorHistoryRequestPacket
        (String sensor, long startDate, long endDate) implements WsPacket {

    public void handle(PacketContext ctx) {
        List<SensorDatabaseWriter.HistoryEntry> data = SensorDatabaseWriter.getSensorDataRange(sensor, startDate, endDate, 5000);
        System.out.println("Sensor data range: " + data.size());
        final int CHUNK_SIZE = 500;
        boolean clear = true;


        for (int i = 0; i < data.size(); i += CHUNK_SIZE) {
            if (!ctx.getContext().session.isOpen()) break;
            int end = Math.min(data.size(), i + CHUNK_SIZE);
            List<SensorDatabaseWriter.HistoryEntry> chunk = data.subList(i, end);
            boolean isLast = (end == data.size());
            ctx.send(new SensorHistoryPacket(clear, chunk.toArray(new SensorDatabaseWriter.HistoryEntry[0]), isLast));
            clear = false;
        }
    }
}