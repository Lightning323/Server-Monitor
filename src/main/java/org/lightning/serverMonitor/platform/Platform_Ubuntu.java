package org.lightning.serverMonitor.platform;

import com.sun.management.OperatingSystemMXBean;
import org.lightning.serverMonitor.Main;
import org.lightning.serverMonitor.CustomCommand;
import org.lightning.serverMonitor.utils.MiscUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lightning.serverMonitor.Main.LOGGER;

class Platform_Ubuntu extends Platform {


    public Platform_Ubuntu() {
        super();
    }

    /**
     * System load averages over the past:
     * <p>
     * 1 minute (0.29)
     * <p>
     * 5 minutes (0.15)
     * <p>
     * 15 minutes (0.06)
     *
     * @return
     */
    public String getCPULoadStr() {
        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double processCpuLoad = osBean.getProcessCpuLoad(); // Between 0.0 and 1.0
        String processLoad = "\nProcess CPU Load: " + Math.round(processCpuLoad * 100) + "%";

        try {
            Process process = Runtime.getRuntime().exec("uptime");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    return line +
                            "\n(load avg over the past: 1m, 5m, 15m)" +
                            processLoad;
                }
            }
        } catch (Exception ignored) {
        }
        return "Unknown" + processLoad;
    }

    //TODO: There are 3 ways to get CPU load:
    // 1. Using top command
    // 2. Using /proc/stat
    // 3. Using OperatingSystemMXBean
    //We need to make a decision on which one to use to simplify the code

    private long[] lastStats = null;
    private long lastTimestamp = 0;

    /**
     * Returns the CPU load based on the time elapsed since the last call.
     * Returns 0.0 on the first call to initialize the baseline.
     */
    public double getCPULoad() {
        try {
            long[] currentStats = readCpuStats();
            long currentTimestamp = System.currentTimeMillis();

            if (lastStats == null) {
                lastStats = currentStats;
                lastTimestamp = currentTimestamp;
                return 0.0; // First call, no history to compare against
            }

            double idleDiff = currentStats[0] - lastStats[0];
            double totalDiff = currentStats[1] - lastStats[1];

            // Update state for next call
            lastStats = currentStats;
            lastTimestamp = currentTimestamp;

            // Prevent division by zero
            if (totalDiff <= 0) return 0.0;

            return 100.0 * (1.0 - (idleDiff / totalDiff));
        } catch (IOException e) {
            throw new RuntimeException("Error reading CPU stats", e);
        }
    }

    private long[] readCpuStats() throws IOException {
        // Same implementation as provided previously
        String line = java.nio.file.Files.readAllLines(java.nio.file.Paths.get("/proc/stat")).get(0);
        String[] parts = line.split("\\s+");

        long idleTime = Long.parseLong(parts[4]) + Long.parseLong(parts[5]);
        long totalTime = 0;
        for (int i = 1; i < parts.length; i++) {
            totalTime += Long.parseLong(parts[i]);
        }
        return new long[]{idleTime, totalTime};
    }


    public double getCPULoad2() {
        // We invoke the shell (/bin/sh) to handle the pipes (|) and command features
        String command = "top -bn2 -d 0.1 | grep \"Cpu(s)\" | tail -n1 | awk '{print 100 - $8}'";
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);

        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.isEmpty()) {
                    try {
                        // Parse the output (a single number like "12.5")
                        return Double.parseDouble(line.trim());
                    } catch (NumberFormatException e) {
                        return -1.0;
                    }
                }
            } catch (IOException e) {
                Main.LOGGER.error("Failed to get CPU load", e);
            } finally {
                process.destroy();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to get CPU load", e);
        }
        return -1.0;
    }

    public double getImmediateCPULoad() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double load = osBean.getCpuLoad(); // Returns % as 0.0 to 1.0
        return (load < 0) ? -1 : load * 100; // Returns -1 if not available
    }


    public boolean isRunningAsAdmin() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("id", "-u");
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().equals("0")) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            return false;
        }

        return false;
    }

    protected long pollAwakeMillis() {
        try {
            // Run: uptime -s  (gives something like "2025-02-02 14:33:11")
            Process proc = new ProcessBuilder("uptime", "-s").start();
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = br.readLine().trim();

            // Parse timestamp
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime bootOrResume = LocalDateTime.parse(line, fmt);

            // Now/then difference
            Duration diff = Duration.between(bootOrResume, LocalDateTime.now());

            return diff.toMillis();
        } catch (IOException e) {
            LOGGER.error("Failed to get awake millis", e);
            return -1;
        }
    }

    @Override
    public boolean suspend(String reason) {
        if (super.suspend(reason)) {
            LOGGER.info("Suspending " + (reason == null ? "" : ": " + reason));
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            if (!Main.TEST_MODE) {
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "systemctl suspend");
                    Process process = processBuilder.start();
                    int exit = process.waitFor();
                    return exit == 0;
                } catch (Throwable e) {
                    LOGGER.error("Failed to suspend", e);
                }
            }
        }
        LOGGER.info("Refusing to suspend");
        return false;
    }

    public boolean isUserInactive() {
        try {
            String[] cmd = {
                    "bash",
                    "-c",
                    "loginctl show-session $(loginctl | awk 'NR==2{print $1}') -p IdleHint -p IdleSinceHint"
            };

            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            boolean idle = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("IdleHint=")) {
                    idle = line.contains("yes");
                }
            }

            process.waitFor();
            return idle;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public String getOSRamUsage() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("vmstat", "-s");
            //alternative free -h
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder out = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    String[] parts = line.split("K");
                    // System.out.println(Arrays.toString(parts));

                    if (parts.length > 1) {
                        if (parts[1].trim().equals("total memory")) {
                            long totalMemory = Long.parseLong(parts[0].trim()) * 1024; // Convert KB to bytes
                            out.append("\nTotal memory: ").append(MiscUtils.bytesToGB(totalMemory)).append(" GB");
                        } else if (parts[1].trim().equals("used memory")) {
                            long used = Long.parseLong(parts[0].trim()) * 1024; // Convert KB to bytes
                            out.append("\nUsed memory: ").append(MiscUtils.bytesToGB(used)).append(" GB");
                        }
                    }

                }

                return out.toString();
            } finally {
                process.waitFor();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "unknown";
    }

    protected String[] setMaxFrequencyCommand(double minFrequencyMHZ, double maxFrequencyMHZ) {
        //sudo cpupower frequency-set -d 0 -u 0
        return new String[]{"bash", "-c", "sudo cpupower frequency-set -d " + minFrequencyMHZ + "MHz -u " + maxFrequencyMHZ + "MHz"};
    }

    public String getCPUInfo() {
        String out = "";
        out += "CPU Vendor: " + CPU_VENDOR + "\n";
        if (CPU_VENDOR.equals(CPU_VENDOR_INTEL)) {
            String governor = "Unknown";
            String maxPower = "Unknown";

            try {
                // Read the contents of the files and trim any trailing newlines
                governor = Files.readString(Paths.get("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")).trim();
                maxPower = Files.readString(Paths.get("/sys/devices/system/cpu/intel_pstate/max_perf_pct")).trim();
            } catch (IOException e) {
                // Handle exception (e.g., file not found, permission denied)
                System.err.println("Failed to read CPU stats: " + e.getMessage());
            }
            out += "Governor: " + governor + " | Max Power: " + maxPower + "%\n";
        }
        FrequencyPolicy policy = getFrequencyPolicy();
        out += "Hardware Frequency: " + policy.minHardwareFrequencyMHZ + "MHz - " + policy.maxHardwareFrequencyMHZ + "MHz\n";
        out += "Software Frequency: " + policy.minSoftwareFrequencyMHZ + "MHz - " + policy.maxSoftwareFrequencyMHZ + "MHz\n";
        return out;
    }

    public String getCPUVendor() {
        try {
            // Read lines from /proc/cpuinfo
            for (String line : Files.readAllLines(Paths.get("/proc/cpuinfo"))) {
                if (line.startsWith("vendor_id")) {
                    if (line.contains("GenuineIntel")) {
                        return "Intel";
                    } else if (line.contains("AuthenticAMD")) {
                        return "AMD";
                    }
                    return line.split(":")[1].trim(); // Returns raw vendor string if something else
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read /proc/cpuinfo: " + e.getMessage());
        }
        return "Unknown";
    }

    public FrequencyPolicy setMaxFrequencyMHZ(double maxFrequencyMHZ) {
        //Clamp the frequency to hardware limits
        final int margin = 0;
        if (maxFrequencyMHZ < minHardwareFrequencyMHZ + margin)
            maxFrequencyMHZ = minHardwareFrequencyMHZ + margin;
        if (maxFrequencyMHZ > maxHardwareFrequencyMHZ - margin)
            maxFrequencyMHZ = maxHardwareFrequencyMHZ - margin;


        //Start the process
        ProcessBuilder processBuilder = new ProcessBuilder(setMaxFrequencyCommand(minHardwareFrequencyMHZ, maxFrequencyMHZ));
        System.out.println("Frequency range set to: " + minHardwareFrequencyMHZ + "MHz - " + maxFrequencyMHZ + "MHz" +
                "\n" + processBuilder.command());

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
        } catch (Exception e) { // Catch any other unexpected exceptions from Future.get()
            System.err.println("An unexpected error occurred during command execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            //Evaluate if we succeeded or not and Cache the frequency policy
            FrequencyPolicy frequencyPolicy = getFrequencyPolicy();
            cacheFrequencyPolicy(frequencyPolicy);
            return frequencyPolicy;
        }
    }

    public int runAppCustomCommand(CustomCommand command, Consumer<String> str) {
        try {
            System.out.println("Executing command: " + command.command);
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command.command);
            // Redirect error stream to standard output (optional, but good for debugging)
            pb.redirectErrorStream(true);

            // Start the process
            Process process = pb.start();
            System.out.println("Process started with PID: " + process.pid());

            // Read the output from the process's input stream
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                str.accept(line);
            }

            // Wait for the process to complete and get its exit code
            return process.waitFor();
        } catch (Exception e) {
            str.accept("Error executing command: " + e.getMessage());
            LOGGER.info("Custom command failed: ```" + e.getMessage() + "```");
        }
        return -1;
    }

    public void shutdown(String reason) {
        LOGGER.info("Shutting down" + (reason == null ? "" : ": " + reason));
        if (Main.TEST_MODE) {
            System.exit(0);
        } else {
            //Try 3 times to shutdown
            String err = "";
            int attempts = 0;
            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(5000);
                    ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "sudo poweroff");
                    //alternative is shutdown -h now
                    //https://superuser.com/questions/282774/linux-shutdown-in-30-seconds
                    Process process = processBuilder.start();

                    int exitCode = process.waitFor();
                    System.out.println("Shutdown command executed with exit code: " + exitCode);
                    // Optionally, you can check the exit code for success (usually 0)
                    if (exitCode == 0) { //If we successfully shutdown
                        return;
                    } else {
                        System.err.println("Shutdown command might have failed.");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                        String line;

                        while ((line = reader.readLine()) != null) {
                            System.err.println(line);
                            err = line;
                        }
                        attempts++;
                    }
                } catch (InterruptedException | IOException e) {
                    LOGGER.error("Error in shutdown attempt", e);
                }
            }
            LOGGER.info("Shutdown command might have failed. attempts: " + attempts + "\n`" + err + "`");
        }
    }


    @Override
    public FrequencyPolicy getFrequencyPolicy() {
        /**
         * Get Frequency Policy (At least the important parts of it)
         */
        String rawCommandOutput = null;
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "cpupower -c all frequency-info");
        System.out.println(processBuilder.command());
        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    //System.out.println(line);
                    String formattedLine = line.trim().toLowerCase();
                    if (formattedLine.startsWith("current policy:")) {
                        output.append("\t").append(line).append("\n");
                    } else if (formattedLine.startsWith("hardware limits:")) {
                        output.append("\t").append(line).append("\n");
                    } else if (formattedLine.startsWith("analyzing cpu")) {
                        output.append(line).append("\n");
                    }
                }
                rawCommandOutput = output.toString();
            } catch (IOException e) {
                // Log the error for stdout stream reading
                System.err.println("Error reading standard output stream: " + e.getMessage());
                // Optionally, rethrow as a RuntimeException if you want to fail the task
                // throw new RuntimeException(e);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Command execution failed with exit code: " + exitCode);
                return null;
            }
        } catch (Exception e) { // Catch any other unexpected exceptions from Future.get()
            System.err.println("An unexpected error occurred during command execution: " + e.getMessage());
            e.printStackTrace();
        }

        if (rawCommandOutput == null) {
            return null;
        }

        /**
         * Parse the data into useful values
         */
        double highestMinHardwareMhz = 0;
        double highestMaxHardwareMhz = 0;
        double highestMinSoftwareMhz = 0;
        double highestMaxSoftwareMhz = 0;

        Pattern hardwarePattern = Pattern.compile("hardware limits: (\\d+(\\.\\d+)?)\\s*(MHz|GHz)\\s*-\\s*(\\d+(\\.\\d+)?)\\s*(MHz|GHz)");
        Pattern policyPattern = Pattern.compile("current policy: frequency should be within (\\d+(\\.\\d+)?)\\s*(MHz|GHz)\\s*and\\s*(\\d+(\\.\\d+)?)\\s*(MHz|GHz)");

        String[] lines = rawCommandOutput.split("\n");

        for (String line : lines) {
            Matcher hardwareMatcher = hardwarePattern.matcher(line);
            if (hardwareMatcher.find()) {
                double minHardware = convertToMhz(Double.parseDouble(hardwareMatcher.group(1)), hardwareMatcher.group(3));
                double maxHardware = convertToMhz(Double.parseDouble(hardwareMatcher.group(4)), hardwareMatcher.group(6));

                if (minHardware > highestMinHardwareMhz) {
                    highestMinHardwareMhz = minHardware;
                }
                if (maxHardware > highestMaxHardwareMhz) {
                    highestMaxHardwareMhz = maxHardware;
                }
            }

            Matcher policyMatcher = policyPattern.matcher(line);
            if (policyMatcher.find()) {
                double minSoftware = convertToMhz(Double.parseDouble(policyMatcher.group(1)), policyMatcher.group(3));
                double maxSoftware = convertToMhz(Double.parseDouble(policyMatcher.group(4)), policyMatcher.group(6));

                if (minSoftware > highestMinSoftwareMhz) {
                    highestMinSoftwareMhz = minSoftware;
                }
                if (maxSoftware > highestMaxSoftwareMhz) {
                    highestMaxSoftwareMhz = maxSoftware;
                }
            }
        }

        System.out.println("maximum hardware frequency: " + highestMaxHardwareMhz + " MHz;\tmaximum software frequency: " + highestMaxSoftwareMhz + " MHz");

        /**
         * We still keep the raw output for checking to make sure it's correct
         */
        FrequencyPolicy frequencyPolicy = new FrequencyPolicy(
                highestMinSoftwareMhz, highestMaxSoftwareMhz,
                highestMinHardwareMhz, highestMaxHardwareMhz,
                rawCommandOutput
        );
        cacheFrequencyPolicy(frequencyPolicy);
        return frequencyPolicy;
    }

    private static double convertToMhz(double value, String unit) {
        if (unit.equalsIgnoreCase("GHz")) {
            return value * 1000;
        }
        return value;
    }

}
