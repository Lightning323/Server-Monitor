package org.pcMonitor.platform;

import com.sun.management.OperatingSystemMXBean;
import org.pcMonitor.Main;
import org.pcMonitor.CustomCommand;
import org.pcMonitor.utils.Logging;
import org.pcMonitor.utils.MiscUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Platform_Ubuntu extends Platform {

    CpuMonitorService cpuMonitorService;

    public Platform_Ubuntu() {
        super();
        cpuMonitorService = new CpuMonitorService(5);
        cpuMonitorService.start(Main.settings.SENSORS_UPDATE_MS);//Samples every second
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

    public double getImmediateCPULoad() {
        return Math.round(cpuMonitorService.getSmoothedCpuLoad());
    }

    /**
     * High Latency:
     * top -bn1 takes ~100ms to run. That’s ~100× slower than /proc/stat.
     * <p>
     * Low Precision:
     * top only reports rounded CPU usage (2 decimal digits max) and averages over 100ms.
     * <p>
     * Expensive Fork:
     * Every call to top spawns a full shell + terminal environment.
     *
     * @return
     */
    public double getTopCPULoad() {
        double cpuLoad = -1.0;
        try {
            Process process = new ProcessBuilder("top", "-bn1").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    if (line.contains("%Cpu(s):")) {
                        String[] parts = line.substring("%Cpu(s):".length()).split("\\s+");
                        for (int i = 1; i < parts.length; i++) {
                            if (parts[i].endsWith("us,")) {//Usage
                                String usage = parts[i - 1];
//                                System.out.println(usage);
                                try {
                                    return Double.parseDouble(usage);
                                } catch (NumberFormatException e) {
                                    // Handle parsing error if needed
                                    e.printStackTrace();
                                }
                            }
                        }
                        break; // Found the CPU line, no need to read further
                    }
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return cpuLoad;
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
            Logging.error(e, "Failed to get awake millis");
            return -1;
        }
    }

    @Override
    public boolean suspend(String reason) {
        if (super.suspend(reason)) {
            Logging.log("Suspending " + (reason == null ? "" : ": " + reason));
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
                    Logging.error(e, "Failed to suspend");
                }
            }
        }
        Logging.log("Refusing to suspend");
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

    private static final String CPU_TEMP_CMD = "sensors";
    private static final String CPU_TEMP_SENSOR_NAME = "k10temp-pci-00c3";
    private static final String CPU_TEMP_SENSOR_PREFIX = "Tctl:";
    private static final Pattern TEMP_PATTERN = Pattern.compile(CPU_TEMP_SENSOR_PREFIX + "\\s+\\+(\\d+\\.\\d+)");

    protected String[] setMaxFrequencyCommand(double minFrequencyMHZ, double maxFrequencyMHZ) {
        //sudo cpupower frequency-set -d 0 -u 0
        return new String[]{"bash", "-c", "sudo cpupower frequency-set -d " + minFrequencyMHZ + "MHz -u " + maxFrequencyMHZ + "MHz"};
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

    public double getCPUTempCelsius() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("sensors");
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                boolean foundSensor = false;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (foundSensor) {
                        Matcher matcher = TEMP_PATTERN.matcher(line.trim());
                        if (matcher.find()) {
                            try {
                                return Double.parseDouble(matcher.group(1));
                            } catch (NumberFormatException e) {
                                // Handle parsing error if needed
                                return -1;
                            }
                        }
                    } else if (line.trim().equals(CPU_TEMP_SENSOR_NAME)) {
                        foundSensor = true;
                        continue;
                    }
                }
            } finally {
                process.waitFor();
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot read CPU temp", e);
        }
        return -1;
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
            Logging.log("Custom command failed: ```" + e.getMessage() + "```");
        }
        return -1;
    }

    public void shutdown(String reason) {
        Logging.log("Shutting down" + (reason == null ? "" : ": " + reason));
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
                        if (Main.bot != null) Main.bot.getShardManager().shutdown();
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
                    Logging.error(e);
                }
            }
            Logging.log("Shutdown command might have failed. attempts: " + attempts + "\n`" + err + "`");
        }
    }

    private static void killAll() throws IOException, InterruptedException {
        //Kill all processes if we have time
        //https://superuser.com/questions/161531/how-to-kill-all-processes-in-linux
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "killall5 -9");
        Process process = processBuilder.start();
        process.waitFor();
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
