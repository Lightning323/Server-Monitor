package org.lightning.serverMonitor.sensorMonitoring;

import org.lightning.serverMonitor.Main;
import org.lightning.serverMonitor.utils.MiscUtils;

import java.util.Map;
import java.util.TreeMap;
import java.util.List;

public class TimeOverTemp {
    // We use a TreeMap to keep the keys (thresholds) sorted automatically
    private final TreeMap<Integer, Long> timeMap = new TreeMap<>();

    /**
     *
     * @param thresholds List of thresholds eg [45, 50, 55, 60] etc
     */
    public TimeOverTemp(List<Integer> thresholds) {
        for (Integer t : thresholds) {
            timeMap.put(t, 0L);
        }
    }

    public String getTimeOverTemp() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Long> entry : timeMap.entrySet()) {
            sb.append("> ").append(entry.getKey()).append("C: ")
                    .append(MiscUtils.convertMsToHMS(entry.getValue())).append("\n");
        }
        return sb.toString();
    }

    public void accumulate(double cpuTemp, int timeSinceLastSample) {
        for (Integer threshold : timeMap.keySet()) {
            if (cpuTemp > threshold) {
                timeMap.put(threshold, timeMap.get(threshold) + timeSinceLastSample);
            }
        }
    }
}