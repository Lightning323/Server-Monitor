package org.lightning.serverMonitor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Settings {


    public String SERVER_NAME = "Server";

    //Webapp
    public int WEBAPP_LOCALHOST_PORT = 8080;

    //Notifications
    public String DISCORD_WEBHOOK_URL = "password";

    //Sensors
    public int SENSORS_UPDATE_MS = 5000;
    public int STARTUP_FREQUENCY_MHZ = 3000;

    //Linux LM-sensors
    public String LINUX_CPU_TEMP_SENSOR_NAME = "k10temp-pci-00c3";
    public String LINUX_CPU_TEMP_KEY = "Tctl";

    //Temprature alert / protection
    public long PROTECTION_ALERT_NOTIFICATION_INTERVAL = 1000 * 60 * 5;
    public boolean PROTECTION_SHUTDOWN_ON_TEMP_ERROR = false;
    public int PROTECTION_SHUTDOWN_TEMP = 90;
    public int PROTECTION_ALERT_TEMP = 70;


    //========================================================================================================================
    //========================================================================================================================
    //========================================================================================================================
    // === Internal stuff ===
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SETTINGS_PATH = Paths.get(System.getProperty("user.dir"), "settings.json");

    public static void save(Settings settings) {
        try (Writer writer = Files.newBufferedWriter(SETTINGS_PATH)) {
            GSON.toJson(settings, writer);
        } catch (Throwable e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    public static Settings load() {
        System.out.println("Loading settings from "+SETTINGS_PATH.toString());
        if (Files.exists(SETTINGS_PATH)) {
            try (Reader reader = Files.newBufferedReader(SETTINGS_PATH)) {
                return GSON.fromJson(reader, (Type) Settings.class);

            } catch (Throwable e) {
                System.err.println("Error loading settings: " + e.getMessage());
            }
        } else {
            save(new Settings()); // Save defaults if no file
        }
        return new Settings();
    }

}
