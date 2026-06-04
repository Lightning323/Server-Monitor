package org.lightning.serverMonitor.platform;

public class FrequencyPolicy {
    public final double minHardwareFrequencyMHZ, maxHardwareFrequencyMHZ, minSoftwareFrequencyMHZ, maxSoftwareFrequencyMHZ;


    public FrequencyPolicy(double highestMinSoftwareMhz, double highestMaxSoftwareMhz, double highestMinHardwareMhz, double highestMaxHardwareMhz) {
        this.maxHardwareFrequencyMHZ = highestMaxHardwareMhz;
        this.minHardwareFrequencyMHZ = highestMinHardwareMhz;
        this.maxSoftwareFrequencyMHZ = highestMaxSoftwareMhz;
        this.minSoftwareFrequencyMHZ = highestMinSoftwareMhz;
    }

    public String toString() {
        return String.format("Hardware limits: %.2f - %.2f MHz\nSoftware limits: %.2f - %.2f MHz",
                minHardwareFrequencyMHZ, maxHardwareFrequencyMHZ, minSoftwareFrequencyMHZ, maxSoftwareFrequencyMHZ);
    }
}
