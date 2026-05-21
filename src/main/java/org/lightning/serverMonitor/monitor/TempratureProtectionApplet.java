package org.lightning.serverMonitor.monitor;

import org.lightning.serverMonitor.Main;
import org.lightning.serverMonitor.platform.FrequencyPolicy;
import org.lightning.serverMonitor.platform.Platform;

public class TempratureProtectionApplet {
    private static long lastDiscordNotification = 0;

    public static void checkTemp(double cpuTemp) {
        if (cpuTemp >= Main.settings.PROTECTION_SHUTDOWN_TEMP && Main.settings.PROTECTION_SHUTDOWN_TEMP > 0) {
            Platform.SINGLETON.shutdown("Temperature too high: " + cpuTemp + "C; Shutting down!");
        } else if (cpuTemp >= Main.settings.PROTECTION_ALERT_TEMP && Main.settings.PROTECTION_ALERT_TEMP > 0) {
            if (Main.settings.PROTECTION_ALERT_NOTIFICATION_INTERVAL > 0 &&
                    System.currentTimeMillis() - lastDiscordNotification > Main.settings.PROTECTION_ALERT_NOTIFICATION_INTERVAL) {
                lastDiscordNotification = System.currentTimeMillis();
                Main.LOGGER.warn("Temperature alert: " + cpuTemp + "C; Downclocking! ");
                downclock();
            }
        }
    }

    public static boolean downclock() {
        try {
            double newFrequency = 0;//Main.settings.PROTECTION_DOWNCLOCK_MHZ;
            FrequencyPolicy frequencyPolicy = Platform.SINGLETON.getFrequencyPolicy();
            if (newFrequency >= frequencyPolicy.maxSoftwareFrequencyMHZ) { //If the new frequency is higher what it orginally was, don't do anything
                return true;
            }
            frequencyPolicy = Platform.SINGLETON.setMaxFrequencyMHZ(newFrequency);
            Main.LOGGER.info("Auto-set frequency to " + frequencyPolicy.maxSoftwareFrequencyMHZ + "MHz");
            return frequencyPolicy.maxSoftwareFrequencyMHZ == newFrequency;
        } catch (Exception e) {
            Main.LOGGER.error("Error reducing frequency",e);
            return false;
        }
    }
}
