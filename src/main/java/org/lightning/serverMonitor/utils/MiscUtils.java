package org.lightning.serverMonitor.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;

public class MiscUtils {
    public static String removeLastSubstring(String str, String toRemove) {
        int index = str.lastIndexOf(toRemove);
        if (index == -1) return str;  // If not found, return original

        return str.substring(0, index) + str.substring(index + toRemove.length());
    }

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
                "%d:%02d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60);
        return seconds < 0 ? "-" + positive : positive;
    }

    public static String bytesToGB(long bytes) {
        return String.format("%.2f", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Executes a command, waits for its completion, and returns the exit code and output.
     *
     * @param command The command and its arguments as a list of strings.
     * @return A CommandResult object containing the exit code, standard output, and standard error.
     */
    public static CommandResult executeCommand(String... command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        int exitCode = -1; // Default exit code in case of an exception

        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();
        Process process = null;

        try {
            process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                // Log the error for stdout stream reading
                System.err.println("Error reading standard output stream: " + e.getMessage());
                // Optionally, rethrow as a RuntimeException if you want to fail the task
                // throw new RuntimeException(e);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                }
            } catch (IOException e) {
                // Log the error for stderr stream reading
                System.err.println("Error reading standard error stream: " + e.getMessage());
                // Optionally, rethrow as a RuntimeException
                // throw new RuntimeException(e);
            }
            // Wait for the process to complete
            exitCode = process.waitFor();

        } catch (Exception e) { // Catch any other unexpected exceptions from Future.get()
            System.err.println("An unexpected error occurred during command execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            process.destroy();
        }
        return new CommandResult(exitCode, output.toString(), error.toString());
    }
}
