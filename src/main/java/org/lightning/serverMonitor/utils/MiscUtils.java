package org.lightning.serverMonitor.utils;

import java.time.Duration;

public class MiscUtils {

    /**
     * Converts milliseconds to a human-readable H:M:S string using Java's Duration.
     *
     * @param milliseconds The time in milliseconds.
     * @return A string in the format "HH:MM:SS", or "Invalid input" if
     * the input is negative. Returns "00:00:00" if input is 0.
     */
    public static String convertMsToHMS(long milliseconds) {
        if (milliseconds < 0) {
            return "Invalid input. Please enter a non-negative number.";
        }

        Duration duration = Duration.ofMillis(milliseconds);
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(
                "%dh:%02dm:%02ds",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60);
        return seconds < 0 ? "-" + positive : positive;
    }

    public static String bytesToGB(long bytes) {
        return String.format("%.2f", bytes / (1024.0 * 1024 * 1024));
    }
}
