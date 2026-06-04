package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.monitor.SensorDatabaseWriter;

public class SensorHistoryPacket implements WsPacket {
    public SensorDatabaseWriter.HistoryEntry[] history;
    public int packetNumber;
    public boolean finalPacket;
    public String sensor;

    public SensorHistoryPacket(String sensor, int packetNumber, SensorDatabaseWriter.HistoryEntry[] hist, boolean finalPacket) {
        this.history = hist;
        this.sensor = sensor;
        this.finalPacket = finalPacket;
        this.packetNumber = packetNumber;
    }
}