package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.sensorMonitoring.SensorDump;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SensorDumpPacket implements WsPacket {
    public List<SensorPacketValue> dumps = new ArrayList<>();

    public SensorDumpPacket(SensorDump dump2) {
        dumps.add(new SensorPacketValue(dump2));
    }

    public SensorDumpPacket(List<SensorDump> dump2) {
        List<SensorPacketValue> newDumps = dump2.stream()
                .map(SensorPacketValue::new)
                .toList();
        dumps.addAll(newDumps);
    }
}