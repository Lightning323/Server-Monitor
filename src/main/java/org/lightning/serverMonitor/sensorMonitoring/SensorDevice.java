package org.lightning.serverMonitor.sensorMonitoring;

import java.util.ArrayList;
import java.util.List;

public class SensorDevice {
    public String deviceName;
    public String adapter;
    public List<SensorProperty> properties = new ArrayList<>();


    @Override
    public String toString() {
        return "SensorDevice{" +
                "deviceName='" + deviceName + '\'' +
                ", adapter='" + adapter + '\'' +
                ", properties=" + properties +
                "}\n";
    }
}