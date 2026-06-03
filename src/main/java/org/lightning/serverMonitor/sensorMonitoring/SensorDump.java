package org.lightning.serverMonitor.sensorMonitoring;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SensorDump {
    public List<SensorDevice> devices = new ArrayList<>();
    public long timestamp;

    public SensorDump(List<SensorDevice> devices, long timestamp) {
        this.devices = devices;
        this.timestamp = timestamp;
    }

    public static SensorDump read() {
        List<SensorDevice> devices = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("sensors").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                SensorDevice currentDevice = null;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    // Identify a new device block (does not contain ":")
                    if (!line.contains(":")) {
                        currentDevice = new SensorDevice();
                        currentDevice.deviceName = line;
                        devices.add(currentDevice);
                    }
                    // Identify adapter
                    else if (line.startsWith("Adapter:")) {
                        if (currentDevice != null) currentDevice.adapter = line.substring(8).trim();
                    }
                    // Identify properties (key: value)
                    else if (currentDevice != null) {
                        String[] parts = line.split(":", 2);
                        String key = parts[0].trim();
                        String value = parts[1].trim();

                        // Filter out extra text like (min = ..., max = ...) if desired
                        if (value.contains("(")) {
                            value = value.substring(0, value.indexOf("(")).trim();
                        }

                        currentDevice.properties.add(new SensorProperty(key, value));
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Error parsing sensors output", e);
        }
        return new SensorDump(devices, System.currentTimeMillis());
    }

    public SensorProperty getSensor(String sensorName, String targetKey) {
        for (SensorDevice device : devices) {
            if (device.deviceName.equals(sensorName)) {
                for (SensorProperty prop : device.properties) {
//                    System.out.println(prop.key + ": " + prop.value);
                    if (prop.key.equals(targetKey)) {
                        return prop;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "devices=" + devices +
                ", timestamp=" + timestamp +
                '}';
    }
}



