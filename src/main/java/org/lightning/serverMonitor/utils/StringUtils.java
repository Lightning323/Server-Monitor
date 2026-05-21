package org.lightning.serverMonitor.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    // Matches: optional sign, digits, optional decimal point and digits
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("([-+]?[0-9]*\\.?[0-9]+)");

    /**
     * Extracts a double from a string containing mixed units/symbols.
     * Returns -1.0 if no valid number is found.
     */
    public static double extractDouble(String value) {
        if (value == null || value.isEmpty()) {
            return -1.0;
        }

        Matcher matcher = NUMERIC_PATTERN.matcher(value);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1.0;
            }
        }

        return -1.0;
    }
}
