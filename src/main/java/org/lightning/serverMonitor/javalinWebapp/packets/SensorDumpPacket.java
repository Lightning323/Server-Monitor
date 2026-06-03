package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.sensorMonitoring.SensorDump;

public class SensorDumpPacket implements WsPacket {
    public SensorPacketValue dump;

    public SensorDumpPacket(SensorDump dump2) {
        dump = new SensorPacketValue(dump2);
    }
}