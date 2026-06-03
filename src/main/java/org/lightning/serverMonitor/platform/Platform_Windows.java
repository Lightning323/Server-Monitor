package org.lightning.serverMonitor.platform;

import org.lightning.serverMonitor.Main;
import org.lightning.serverMonitor.CustomCommand;
import org.lightning.serverMonitor.utils.MiscUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.function.Consumer;

import com.sun.management.OperatingSystemMXBean;

class Platform_Windows extends Platform {

    CoreTempInterface coreTempInterface;

    public Platform_Windows() {
        super();
        try {
            coreTempInterface = new CoreTempInterface(5200);
        } catch (IOException e) {
            System.out.println("Failed to initialize CoreTempInterface");
            Runtime.getRuntime().exit(1);
        }
    }


    public FrequencyPolicy setMaxFrequencyMHZ(double maxFrequencyMHZ) {

        int mhz = (int) maxFrequencyMHZ;

        // If requested frequency is >= hardware max, reset to unlimited
        runCommand("powercfg -attributes SUB_PROCESSOR PROCFREQMAX -ATTRIB_HIDE");
        if (mhz <= 0) {
            runCommand("powercfg -setacvalueindex SCHEME_CURRENT SUB_PROCESSOR PROCFREQMAX 0");
            runCommand("powercfg -setactive SCHEME_CURRENT");
            System.out.println("Resetting CPU max frequency to unlimited");
        } else {
            runCommand("powercfg -setacvalueindex SCHEME_CURRENT SUB_PROCESSOR PROCFREQMAX " + mhz);
            runCommand("powercfg -setactive SCHEME_CURRENT");
            System.out.println("Setting CPU max frequency to " + mhz + " MHz");
        }

        FrequencyPolicy policy = getFrequencyPolicy();
        cacheFrequencyPolicy(policy);
        return policy;
    }

    public int runAppCustomCommand(CustomCommand command, Consumer<String> str) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command.command);
            //Get output from error and output
            Process process = processBuilder.redirectErrorStream(true).start();
            BufferedReader b = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = b.readLine()) != null) {
                str.accept(line);
            }
            return process.waitFor();
        } catch (Exception e) {
            str.accept("Error executing command: " + e.getMessage());
            Main.LOGGER.warn("Custom command failed: ```" + e.getMessage() + "```",e);
        }
        return -1;
    }

    public String getCPULoadStr() {
        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        double systemCpuLoad = osBean.getCpuLoad(); // Between 0.0 and 1.0
        double processCpuLoad = osBean.getProcessCpuLoad(); // Between 0.0 and 1.0

        return "System CPU Load: " + Math.round(systemCpuLoad * 100) + "%" +
                "\nProcess CPU Load: " + Math.round(processCpuLoad * 100) + "%";
    }

    public double getImmediateCPULoad() {
        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        double systemCpuLoad = osBean.getCpuLoad(); // Between 0.0 and 1.0
        return systemCpuLoad * 100;
    }

    public String getOSRamUsage() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("wmic", "OS", "get", "TotalVisibleMemorySize,FreePhysicalMemory", "/VALUE");
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            long totalMemory = 0, freeMemory = 0;

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
//                System.out.println(line);
                if (line.startsWith("TotalVisibleMemorySize=")) {
                    totalMemory = Long.parseLong(line.split("=")[1]) * 1024; // Convert KB to bytes
                } else if (line.startsWith("FreePhysicalMemory=")) {
                    freeMemory = Long.parseLong(line.split("=")[1]) * 1024; // Convert KB to bytes
                }
            }
            String out = "";
            out += ("Total OS RAM: " + MiscUtils.bytesToGB(totalMemory) + " GB");
            out += ("\nFree OS RAM: " + MiscUtils.bytesToGB(freeMemory) + " GB");
            return out;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public double getCPUTempCelsius() {
        if (coreTempInterface != null &&
                coreTempInterface.isConnected()) {
            double val = coreTempInterface.getLatestSystemInfo().CpuInfo.fTemp.get(0);
            return val;
        } else return -1;
    }

    public void shutdown(String reason) {
//        Main.LOGGER.info("Shutting down" + (reason == null ? "" : ": " + reason));
//        if (Main.TEST_MODE) {
//            System.exit(0);
//        } else {
//            //Try 3 times to shutdown
//            String err = "";
//            int attempts = 0;
//            for (int i = 0; i < 3; i++) {
//                try {
//                    Thread.sleep(5000);
//                    ProcessBuilder processBuilder = new ProcessBuilder("shutdown", "/p");
//                    Process process = processBuilder.start();
//                    int exitCode = process.waitFor();
//                    System.out.println("Shutdown command executed with exit code: " + exitCode);
//                    // Optionally, you can check the exit code for success (usually 0)
//                    if (exitCode == 0) { //If we successfully shutdown
//                        return;
//                    } else {
//                        System.err.println("Shutdown command might have failed.");
//                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//                        String line;
//
//                        while ((line = reader.readLine()) != null) {
//                            System.err.println(line);
//                            err = line;
//                        }
//                        attempts++;
//                    }
//                } catch (InterruptedException | IOException e) {
//                    Main.LOGGER.error("Failed shutdown attempt",e);
//                }
//            }
//            Main.LOGGER.info("Shutdown command might have failed. attempts: " + attempts + "\n`" + err + "`");
//        }
    }

    @Override
    public boolean isRunningAsAdmin() {
        try {
            Process process = Runtime.getRuntime().exec("whoami /groups");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // SID for administrators group is S-1-5-32-544
                if (line.contains("S-1-5-32-544")) {
                    return true;
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean suspend(String reason) {
        throw new IllegalArgumentException("Not implemented");
    }

    private double getMaxSoftwareFrequencyMHz() {
        String cmd =
                "powershell.exe -Command \"(powercfg -query SCHEME_CURRENT SUB_PROCESSOR PROCFREQMAX | " +
                        "Select-String -Pattern 'Current AC Power Setting Index') -replace '.*0x','' | " +
                        "ForEach-Object { [convert]::ToInt32($_,16) }\"";

        String output = runCommand(cmd).trim();

        if (output.matches("\\d+")) {
            return Double.parseDouble(output);
        }
        return -1; // 0 = unlimited or unsupported
    }

    @Override
    public FrequencyPolicy getFrequencyPolicy() {
        String rawOutput = "";
        // 1. Get hardware max frequency (MHz)
        String hardwareCmd = "wmic cpu get MaxClockSpeed";
        String hardwareOutput = runCommand(hardwareCmd);
        rawOutput += "0 max software frequency = unlimited\nHardware command output:\n" + hardwareOutput + "\n";

        double maxHardwareMHz = parseMaxClockSpeed(hardwareOutput);
        double minHardwareMHz = 0; // Windows does not expose min hardware freq

        double maxSoftwareMHz = getMaxSoftwareFrequencyMHz();
        double minSoftwareMHz = 0; // Windows does not expose min software freq

        System.out.println("maximum hardware frequency: " + maxHardwareMHz + " MHz; maximum software frequency: " + maxSoftwareMHz + " MHz");

        FrequencyPolicy policy = new FrequencyPolicy(
                minSoftwareMHz, maxSoftwareMHz,
                minHardwareMHz, maxHardwareMHz,
                rawOutput
        );

        cacheFrequencyPolicy(policy);
        return policy;
    }


    private String runCommand(String cmd) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    private double parseMaxClockSpeed(String output) {
        // Example output:
        // MaxClockSpeed
        // 4200
        for (String line : output.split("\n")) {
            line = line.trim();
            if (line.matches("\\d+")) {
                return Double.parseDouble(line);
            }
        }
        return 0;
    }

}
