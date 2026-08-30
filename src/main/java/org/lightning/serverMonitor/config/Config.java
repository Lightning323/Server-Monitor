package org.lightning.serverMonitor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.lightning.serverMonitor.sensorMonitoring.SensorDump;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.lightning.serverMonitor.Main.LOGGER;

public class Config {
    public String SERVER_NAME = "Server";
    public int WEBAPP_PORT = 3000;

    public String DISCORD_WEBHOOK_URL = "";
    public long TEMP_NOTIFICATION_MS = 30 * 1000;
    public int SENSORS_UPDATE_MS = 1000;
    public long METRICS_UPDATE_MS = 1 * 60 * 1000;
    public TreeMap<String, SensorConfigProperty> SENSORS = new TreeMap<>();
    public long DATABASE_RECORD_WRITE_INTERVAL_MS = 5 * 60 * 1000;
    public List<TaskbarSensor> TASKBAR_SENSORS = new ArrayList<>();

    public record TaskbarSensor(String alias, String sensorId){};

    public record SensorConfigProperty(
            String alias,
            String unit,
            int notificationThreshold
    ) {
        public boolean notifyTemp() {
            return notificationThreshold > 0;
        }

        public boolean isTempSensor() {
            return unit.toLowerCase().contains("c");
        }
    }


    //========================================================================================================================
    //========================================================================================================================
    //========================================================================================================================


    // === Internal stuff ===
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SETTINGS_PATH = Paths.get(System.getProperty("user.dir"), "config.json");

    public static void save(Config settings) {
        try (Writer writer = Files.newBufferedWriter(SETTINGS_PATH)) {
            GSON.toJson(settings, writer);
        } catch (Throwable e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    public static Config load() {
        if (Files.exists(SETTINGS_PATH)) {
            LOGGER.info("Loading settings from " + SETTINGS_PATH.toString());
            try (Reader reader = Files.newBufferedReader(SETTINGS_PATH)) {
                return GSON.fromJson(reader, (Type) Config.class);
            } catch (Throwable e) {
                System.err.println("Error loading settings: " + e.getMessage());
            }
        }
        LOGGER.info("No settings file found, creating default");
        Config config = new Config();
        SensorDump sensors = SensorDump.read();
        final Pattern unitPattern = Pattern.compile("(?<=[^a-zA-Z])[^0-9.]+", Pattern.MULTILINE);
        sensors.forEachSensor((sensorName, value) -> {
            final Matcher matcher = unitPattern.matcher(value);
            String unit = "";
            LOGGER.debug("Sensor: " + sensorName + ", Value: " + value);
            if (matcher.find()) unit = matcher.group(0).trim().toUpperCase();
            String alias = sensorName
                    .replaceAll("__(.*?)__", " ($1) ")
                    .replace('_', ' ')
                    .trim();
            alias = Arrays.stream(alias.split("\\s+"))
                    .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                    .collect(Collectors.joining(" "));
            config.SENSORS.put(sensorName, new SensorConfigProperty(alias, unit, -1));
        });
        save(config); // Save defaults if no file
        return config;
    }

}
