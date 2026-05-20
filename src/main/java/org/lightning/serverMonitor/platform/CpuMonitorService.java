package org.lightning.serverMonitor.platform;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

 class CpuMonitorService {
    private final double[] buffer;
    private final int maxSamples;
    private int index = 0;
    private int count = 0;

    private long lastTotal = 0;
    private long lastIdle = 0;

    private Thread worker;
    private volatile boolean running = false;
    private volatile double latestCpu = -1.0;

    public CpuMonitorService(int maxSamples) {
        this.maxSamples = maxSamples;
        this.buffer = new double[maxSamples];
    }

    public void start(long sampleIntervalMillis) {
        if (running) return;
        running = true;
        worker = new Thread(() -> {
            while (running) {
                sample();
                try {
                    Thread.sleep(sampleIntervalMillis);
                } catch (InterruptedException ignored) {}
            }
        }, "CpuMonitorThread");
        worker.setDaemon(true);
        worker.start();
    }

    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
    }

    public void shutdown() {
        stop();
    }

     /**
      * Returns the average CPU load over the last X seconds
      * @return
      */
    public double getSmoothedCpuLoad() {
        if (count == 0) return -1.0;
        double sum = 0.0;
        for (int i = 0; i < count; i++) sum += buffer[i];
        return sum / count;
    }

    public double getLatestCpuLoad() {
        return latestCpu;
    }

    private void sample() {
        long[] stat = readCpuStat();
        long total = stat[0];
        long idle = stat[1];

        if (lastTotal != 0 && lastIdle != 0) {
            long totalDiff = total - lastTotal;
            long idleDiff = idle - lastIdle;

            if (totalDiff > 0) {
                double usage = (double)(totalDiff - idleDiff) / totalDiff * 100.0;
                addSample(usage);
            }
        }

        lastTotal = total;
        lastIdle = idle;
    }

    private void addSample(double value) {
        buffer[index] = value;
        index = (index + 1) % maxSamples;
        if (count < maxSamples) count++;
        latestCpu = value;
    }

    private long[] readCpuStat() {
      //  System.out.println("Sample");
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = reader.readLine();
            if (line != null && line.startsWith("cpu ")) {
                int pos = 4; // skip "cpu "
                long[] values = new long[8];
                for (int i = 0; i < 8 && pos < line.length(); i++) {
                    while (pos < line.length() && line.charAt(pos) == ' ') pos++;
                    int start = pos;
                    while (pos < line.length() && line.charAt(pos) != ' ') pos++;
                    String num = line.substring(start, pos);
                    values[i] = Long.parseLong(num);
                }

                long totalIdle = values[3] + values[4]; // idle + iowait
                long total = 0;
                for (int i = 0; i < 8; i++) total += values[i];

                return new long[]{ total, totalIdle };
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new long[]{ 0, 0 };
    }
}
