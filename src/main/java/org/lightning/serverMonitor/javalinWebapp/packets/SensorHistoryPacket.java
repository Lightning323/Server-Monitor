package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.sensorMonitoring.SensorDatabase.HistoryEntry;
import org.lightning.serverMonitor.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SensorHistoryPacket implements WsPacket {
    public HistoryEntry[] history;
    public int packetNumber;
    public boolean finalPacket;
    public String sensor;

    public SensorHistoryPacket(String sensor, int packetNumber, HistoryEntry[] hist, boolean finalPacket) {
        this.history = hist;
        this.sensor = sensor;
        this.finalPacket = finalPacket;
        this.packetNumber = packetNumber;
    }

}