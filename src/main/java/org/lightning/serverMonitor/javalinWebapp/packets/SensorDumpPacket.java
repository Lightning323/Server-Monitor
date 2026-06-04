package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.sensorMonitoring.SensorDump;

import java.util.ArrayList;
import java.util.List;

public class SensorDumpPacket implements WsPacket {
    public List<SensorPacketValue> dumps = new ArrayList<>();

    public SensorDumpPacket(SensorDump dump2) {
        dumps.add(new SensorPacketValue(dump2));
    }

    public SensorDumpPacket(List<SensorDump> dump2) {
        dump2.forEach(d -> dumps.add(new SensorPacketValue(d)));
    }
}