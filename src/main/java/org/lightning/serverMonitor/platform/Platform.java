package org.lightning.serverMonitor.platform;

import org.lightning.serverMonitor.CustomCommand;
import org.lightning.serverMonitor.Main;
import org.lightning.serverMonitor.javalinWebapp.packets.WsPacket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public abstract class Platform {

    public static Platform SINGLETON;
    public final static String OS = System.getProperty("os.name").toLowerCase();
    public static boolean IS_ADMIN = false;

    public final static String CPU_VENDOR_INTEL = "Intel";
    public final static String CPU_VENDOR_AMD = "AMD";
    public final static String CPU_VENDOR_UNKNOWN = "Unknown";

    public final static String CPU_VENDOR;

    static {
        if (OS.toLowerCase().contains("linux")) {
            Platform.SINGLETON = new Platform_Linux();
            IS_ADMIN = Platform.SINGLETON.isRunningAsAdmin();
        } else {
            throw new RuntimeException("Unsupported OS: " + OS);
        }
        System.out.println("Is admin: " + IS_ADMIN);
        System.out.println("OS: " + OS);

        CPU_VENDOR = Platform.SINGLETON.getCPUVendor();
    }


    protected static long START_TIME = System.currentTimeMillis();
    public final double minHardwareFrequencyMHZ, maxHardwareFrequencyMHZ;
    private long cachedFrequency_lastCheck = 0;
    private FrequencyPolicy cachedFrequencyPolicy = null;

    public Platform() {
        cachedFrequencyPolicy = getFrequencyPolicy();
        minHardwareFrequencyMHZ = cachedFrequencyPolicy.minHardwareFrequencyMHZ;
        maxHardwareFrequencyMHZ = cachedFrequencyPolicy.maxHardwareFrequencyMHZ;
    }


    public double getCPULoad() {
        return 0;
    }

    public double getImmediateCPULoad() {
        return 0;
    }

    public abstract String getOSRamUsage();

    public void shutdown(String reason) {
    }

    public int runAppCustomCommand(CustomCommand command, Consumer<String> str) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command.command);
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
            Main.LOGGER.warn("Custom command failed: ```" + e.getMessage() + "```");
        }
        return -1;
    }

    public abstract boolean isRunningAsAdmin();

    private long timeSinceLastSuspend = 0;

    public boolean suspend(String reason) {
        if (System.currentTimeMillis() - timeSinceLastSuspend < 120 * 1000) {
            //We can't just suspend and suspend again and again without the users consent
            return false;
        }
        START_TIME = System.currentTimeMillis();
        timeSinceLastSuspend = System.currentTimeMillis();
        return true;
    }

    public boolean isUserInactive() {
        return false;
    }

    public long getAwakeMillis() {
        return Math.min(
                System.currentTimeMillis() - START_TIME,
                pollAwakeMillis()
        );
    }

    protected long pollAwakeMillis() {
        return -1;
    }

    public WsPacket getSystemInfo() {
        return null;
    }

    protected String getCPUVendor() {
        return "Unknown";
    }

    public abstract FrequencyPolicy setMaxFrequencyMHZ(double maxFrequencyMHZ);

    protected void cacheFrequencyPolicy(FrequencyPolicy frequencyPolicy) {
        cachedFrequencyPolicy = frequencyPolicy;
        cachedFrequency_lastCheck = System.currentTimeMillis();
    }

    /**
     * Designed so that we dont have to get the frequency policy every time we need it
     * it is very unlikely that the policy will change unless we change it ourselves
     *
     * @param CACHE_checkIntervalMs
     * @return
     */
    public final FrequencyPolicy getFrequencyPolicy(long CACHE_checkIntervalMs) {
        if (
                cachedFrequencyPolicy == null ||
                        (CACHE_checkIntervalMs > 0 && System.currentTimeMillis() - cachedFrequency_lastCheck > CACHE_checkIntervalMs)
        ) {
            cachedFrequencyPolicy = getFrequencyPolicy();
        }
        return cachedFrequencyPolicy;
    }

    public abstract FrequencyPolicy getFrequencyPolicy();
}
