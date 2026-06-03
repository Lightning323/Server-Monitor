package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.monitor.SensorDatabaseWriter;

public class SensorHistoryPacket implements WsPacket {
    public SensorDatabaseWriter.HistoryEntry[] history;
    public boolean clear;
    public boolean finalPacket;

    public SensorHistoryPacket( boolean clear, SensorDatabaseWriter.HistoryEntry[] hist, boolean finalPacket) {
        history = hist;
        this.finalPacket = finalPacket;
        this.clear = clear;
    }
}