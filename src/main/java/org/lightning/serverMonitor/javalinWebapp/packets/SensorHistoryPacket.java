package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.monitor.SensorDatabaseWriter;

public class SensorHistoryPacket implements WsPacket {
    public SensorDatabaseWriter.HistoryEntry[] history;

    public SensorHistoryPacket(SensorDatabaseWriter.HistoryEntry[] hist) {
        history = hist;
    }
}