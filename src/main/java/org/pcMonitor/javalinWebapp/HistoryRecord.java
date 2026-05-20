package org.pcMonitor.javalinWebapp;

public class HistoryRecord {
    public static final String RECORD_HEADER = "time,cpu-load,cpu-temp\n";

    public double cpuLoad;
    public double cpuTemp;
    public long time;

    public HistoryRecord(long time, double cpuTemp, double cpuLoad) {
        this.time = time;
        this.cpuTemp = cpuTemp;
        this.cpuLoad = cpuLoad;
    }


    public String toString(String prefix) {
        return prefix + time + "," + cpuLoad + "," + cpuTemp + "\n";
    }

    public String toString() {
        return toString("");
    }

}
