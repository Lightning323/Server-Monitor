package org.pcMonitor.applets.temprature;

import org.pcMonitor.Main;
import org.pcMonitor.platform.FrequencyPolicy;
import org.pcMonitor.platform.Platform;
import org.pcMonitor.utils.Logging;

public class TempratureProtectionApplet {
    private static long lastDiscordNotification = 0;

    public static void checkTemp(double cpuTemp) {
        if (cpuTemp >= Main.settings.PROTECTION_SHUTDOWN_TEMP && Main.settings.PROTECTION_SHUTDOWN_TEMP > 0) {
            Platform.SINGLETON.shutdown("Temperature too high: " + cpuTemp + "C");
        } else if (cpuTemp >= Main.settings.PROTECTION_ALERT_TEMP && Main.settings.PROTECTION_ALERT_TEMP > 0) {
            if (Main.settings.PROTECTION_TEMP_ALERT_DISCORD_NOTIFICATION_INTERVAL > 0 &&
                    System.currentTimeMillis() - lastDiscordNotification > Main.settings.PROTECTION_TEMP_ALERT_DISCORD_NOTIFICATION_INTERVAL) {
                lastDiscordNotification = System.currentTimeMillis();
                Logging.log("Temperature alert: " + cpuTemp + "C");
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
            Logging.log("Auto-set frequency to " + frequencyPolicy.maxSoftwareFrequencyMHZ + "MHz");
            return frequencyPolicy.maxSoftwareFrequencyMHZ == newFrequency;
        } catch (Exception e) {
            Logging.error(e, "Error reducing frequency");
            return false;
        }
    }
}
