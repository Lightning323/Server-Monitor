package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.sensorMonitoring.SensorDump;

import java.util.HashMap;

class SensorPacketValue {
    public long timestamp;
    public HashMap<String, String> sensors;

    public SensorPacketValue(SensorDump dump) {
        this.sensors = new HashMap<>();
        dump.devices.forEach((device) -> {
            device.properties.forEach((property) -> {
                sensors.put(SensorDump.getSensorID(device, property), property.value);
            });
        });
        this.timestamp = dump.timestamp;
    }
}