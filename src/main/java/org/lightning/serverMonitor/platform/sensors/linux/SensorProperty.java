package org.lightning.serverMonitor.platform.sensors.linux;

public  class SensorProperty {
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
                    ", value='" + value + '\'' +
                    '}';
        }
    }