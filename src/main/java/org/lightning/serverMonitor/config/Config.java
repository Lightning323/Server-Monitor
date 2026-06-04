package org.lightning.serverMonitor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.lightning.serverMonitor.platform.Platform;
import org.lightning.serverMonitor.sensorMonitoring.SensorDump;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.lightning.serverMonitor.Main.LOGGER;

public class Config {

    public String SERVER_NAME = "Server";
    //Webapp
    public int WEBAPP_PORT = 3000;
    //Notifications
    public String DISCORD_WEBHOOK_URL = null;
    //Sensors
    public int SENSORS_UPDATE_MS = 1000;

    //    public String LINUX_CPU_TEMP_SENSOR_NAME = "k10temp-pci-00c3";
//    public String LINUX_CPU_TEMP_KEY = "Tctl";
    public HashMap<String, String> SENSOR_ALIASES = new HashMap<>();

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
        sensors.forEachSensor((key, value) -> {
            config.SENSOR_ALIASES.put(key, key);
        });
        save(config); // Save defaults if no file
        return config;
    }

}
