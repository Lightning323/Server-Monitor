package org.lightning.serverMonitor.sensorMonitoring;

public class SensorProperty {
    public String key;
    public String value;

    public SensorProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "SensorProperty{" +
                "key='" + key + '\'' +
                ", v='" + value + '\'' +
                '}';
    }
}