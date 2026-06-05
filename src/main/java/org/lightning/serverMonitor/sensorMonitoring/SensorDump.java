package org.lightning.serverMonitor.sensorMonitoring;

import org.lightning.serverMonitor.platform.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.function.BiConsumer;

public class SensorDump {
    public List<SensorDevice> devices = new ArrayList<>();
    public long timestamp;

    public SensorDump(List<SensorDevice> devices, long timestamp) {
        this.devices = devices;
        this.timestamp = timestamp;
    }

    public static SensorDump read() {
        /**
         *amdgpu-pci-0300
         * Adapter: PCI adapter
         * vddgfx:      571.00 mV
         * fan1:           0 RPM  (min =    0 RPM, max = 3600 RPM)
         * edge:         +31.0°C  (crit = +100.0°C, hyst = -273.1°C)
         *                        (emerg = +105.0°C)
         * junction:     +37.0°C  (crit = +110.0°C, hyst = -273.1°C)
         *                        (emerg = +115.0°C)
         * mem:          +42.0°C  (crit = +108.0°C, hyst = -273.1°C)
         *                        (emerg = +113.0°C)
         * PPT:          14.00 W  (cap = 303.00 W)
         *
         *
         * How we parse lm sensors
         * 1. The device is always the first line
         * 2. once we have the device
         *  1. Get adapter by finding "Adapter:"
         *  2. Get properties by finding a colon and splitting it into "<property>:<value>"
         * 3. A blank line always means we've finished processing the current device
         */
        List<SensorDevice> devices = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("sensors").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                SensorDevice currentDevice = null;

                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        //If we reach an empty line, we've finished processing the current device
                        currentDevice = null;
                        continue;
                    }

                    if (currentDevice == null) { //The device is always the first line
                        currentDevice = new SensorDevice();
                        currentDevice.deviceName = line.replace(":", "").trim();
                        devices.add(currentDevice);
                    } else {
                        String trimmed = line.trim();

                        if (trimmed.startsWith("Adapter:")) {
                            currentDevice.adapter = trimmed.substring(8).trim();
                        }
                        // 3. Handle Property lines (Key: Value)
                        else if (trimmed.contains(":")) {
                            String[] parts = trimmed.split(":", 2);
                            String key = parts[0].trim();
                            String value = parts[1].trim();

                            // Only add if it's not a secondary detail/threshold line
                            // Filter out lines that are just continuations of thresholds
                            if (!key.equalsIgnoreCase("Adapter") && !value.isEmpty()) {
                                // Strip out noise in parentheses
                                if (value.contains("(")) {
                                    value = value.substring(0, value.indexOf("(")).trim();
                                }
                                currentDevice.properties.add(new SensorProperty(key, value));
                            }
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Error parsing sensors output", e);
        }

        //Append additional devices
        double cpuLoad = Platform.SINGLETON.getCPULoad();
        SensorDevice appendedDevice = new SensorDevice();
        appendedDevice.deviceName = "cpu";
        appendedDevice.properties.add(new SensorProperty("load", String.format("%.2f", cpuLoad) + "%"));
        devices.add(appendedDevice);
        return new SensorDump(devices, System.currentTimeMillis());
    }

    public static String getSensorID(SensorDevice device, SensorProperty prop) {
        return ((device.deviceName) + "__"
                + (device.adapter == null || device.adapter.isBlank() ? "" : device.adapter + "__")
                + (prop.key)).replaceAll("[^a-zA-Z0-9_]", "_");
    }

    public void forEachSensor(BiConsumer<String, String> sensor) {
        for (SensorDevice device : devices) {
            for (SensorProperty prop : device.properties) {
                sensor.accept(getSensorID(device, prop), prop.value);
            }
        }
    }

    public TreeMap<String, String> getSensorsAsSortedMap() {
        // TreeMap keeps keys sorted alphabetically automatically
        TreeMap<String, String> sensors = new TreeMap<>();
        forEachSensor(sensors::put);
        return sensors;
    }

    public SensorProperty getSensor(String sensorName, String targetKey) {
        for (SensorDevice device : devices) {
            if (device.deviceName.equals(sensorName)) {
                for (SensorProperty prop : device.properties) {
//                    System.out.println(prop.key + ": " + prop.v);
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
        StringBuilder devices2 = new StringBuilder();
        devices.forEach(device -> {
            devices2.append("\n" + device.toString());
        });
        return "SensorData{" +
                "devices=" + devices2 +
                "\n, t=" + timestamp +
                '}';
    }
}



