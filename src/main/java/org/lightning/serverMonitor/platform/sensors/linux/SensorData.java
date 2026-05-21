package org.lightning.serverMonitor.platform.sensors.linux;

import java.util.ArrayList;
import java.util.List;

public class SensorData {
    public List<SensorDevice> devices = new ArrayList<>();

    @Override
    public String toString() {
        return "SensorData{" +
                "devices=" + devices +
                '}';
    }
}



