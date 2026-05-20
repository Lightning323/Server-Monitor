package org.pcMonitor.platform;

public class FrequencyPolicy {
    public final double minHardwareFrequencyMHZ, maxHardwareFrequencyMHZ, minSoftwareFrequencyMHZ, maxSoftwareFrequencyMHZ;
    public final String rawCommandOutput;

    public FrequencyPolicy(double highestMinSoftwareMhz, double highestMaxSoftwareMhz, double highestMinHardwareMhz, double highestMaxHardwareMhz, String rawData) {
        this.maxHardwareFrequencyMHZ = highestMaxHardwareMhz;
        this.minHardwareFrequencyMHZ = highestMinHardwareMhz;
        this.maxSoftwareFrequencyMHZ = highestMaxSoftwareMhz;
        this.minSoftwareFrequencyMHZ = highestMinSoftwareMhz;
        this.rawCommandOutput = rawData;
    }

    public String toString() {
        return String.format("Hardware limits: %.2f - %.2f MHz\nSoftware limits: %.2f - %.2f MHz",
                minHardwareFrequencyMHZ, maxHardwareFrequencyMHZ, minSoftwareFrequencyMHZ, maxSoftwareFrequencyMHZ);
    }
}
