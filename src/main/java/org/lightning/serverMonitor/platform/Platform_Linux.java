package org.lightning.serverMonitor.platform;

import com.sun.management.OperatingSystemMXBean;
import org.lightning.serverMonitor.Main;
import org.lightning.serverMonitor.javalinWebapp.packets.SystemInfo;
import org.lightning.serverMonitor.javalinWebapp.packets.WsPacket;
import org.lightning.serverMonitor.utils.MiscUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lightning.serverMonitor.Main.LOGGER;

class Platform_Linux extends Platform {


    public Platform_Linux() {
        super();
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

            // Parse t
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
            if (!Main.DEV_ENV) {
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
                return out.toString().trim();
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

    public WsPacket getSystemInfo() {
        SystemInfo packet = new SystemInfo();
        packet.cpuVendor = CPU_VENDOR;
        try {
            if (CPU_VENDOR.equals(CPU_VENDOR_INTEL)) {
                // Intel specific paths
                packet.governor = Files.readString(Paths.get("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")).trim();
                packet.powerState = Files.readString(Paths.get("/sys/devices/system/cpu/intel_pstate/max_perf_pct")).trim();
            } else if (CPU_VENDOR.equalsIgnoreCase(CPU_VENDOR_AMD)) {
                // AMD standard governor path
                packet.governor = Files.readString(Paths.get("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")).trim();

                // AMD power state: Check for amd_pstate driver or cpufreq performance levels
                // Try reading energy_performance_preference first (common in newer kernels)
                Path eppPath = Paths.get("/sys/devices/system/cpu/cpu0/cpufreq/energy_performance_preference");
                if (Files.exists(eppPath)) {
                    packet.powerState = Files.readString(eppPath).trim();
                } else {
                    // Fallback to scaling_governor or base_frequency if necessary
                    packet.powerState = "N/A";
                }
            }
        } catch (IOException e) {
            // Handle exception (e.g., file not found, permission denied)
            System.err.println("Failed to read CPU stats: " + e.getMessage());
        }
        packet.frequencyPolicy = getFrequencyPolicy();
        packet.ramUsage = getOSRamUsage();
        return packet;
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

    public void shutdown(String reason) {
        LOGGER.info("Shutting down" + (reason == null ? "" : ": " + reason));
        if (Main.DEV_ENV) {
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
//        System.out.println(processBuilder.command());
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
        /**
         * We still keep the raw output for checking to make sure it's correct
         */
        FrequencyPolicy frequencyPolicy = new FrequencyPolicy(
                highestMinSoftwareMhz, highestMaxSoftwareMhz,
                highestMinHardwareMhz, highestMaxHardwareMhz
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
