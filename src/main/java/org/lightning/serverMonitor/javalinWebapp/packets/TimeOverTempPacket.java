package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.sensorMonitoring.SensorDatabase;
import org.lightning.serverMonitor.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TimeOverTempPacket implements WsPacket {

    public String sensor;
    public HashMap<String, Long> timeMsOverTemp = new HashMap<>();

    public TimeOverTempPacket(String sensorID, List<SensorDatabase.HistoryEntry> hist) {
        this.sensor = sensorID;
        //Calculate how long we have been over a specific threshold
        if (hist != null || hist.size() > 2) {
            List<Long> timestamps = new ArrayList<>();
            List<Double> values = new ArrayList<>();
            // Convert to columnar format
            for (SensorDatabase.HistoryEntry entry : hist) {
                timestamps.add(entry.t());
                values.add(StringUtils.stringToNumber(entry.v()));
            }
            for (int i = 45; i <= 120; i += 5) {
                timeMsOverTemp.put("" + i, calculateMsoverTemp(timestamps, values, i, 10 * 1000));
            }
        }
    }

    private static long calculateMsoverTemp(List<Long> timestamps, List<Double> vals, double threshold, long maxGapMs) {
        long totalTimeMs = 0;
        for (int i = 1; i < timestamps.size(); i++) {
            long deltaT = timestamps.get(i) - timestamps.get(i - 1);

            // Only add time if the gap is within allowed limits
            if (deltaT <= maxGapMs) {
                // If the previous point was above the threshold, count this duration
                if (vals.get(i - 1) > threshold) {
                    totalTimeMs += deltaT;
                }
            }
        }
        return totalTimeMs;
    }
}
