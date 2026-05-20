package org.pcMonitor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppSettings {

//Webapp
    public int WEBAPP_LOCALHOST_PORT = 8080;

    //Sensors
    public int SENSORS_UPDATE_MS = 5000;
    public int STARTUP_FREQUENCY_MHZ = 3000;

    //Temprature alert / protection
    public long PROTECTION_TEMP_ALERT_DISCORD_NOTIFICATION_INTERVAL = 1000 * 60 * 5;
    public boolean PROTECTION_SHUTDOWN_ON_TEMP_ERROR = false;
    public int PROTECTION_SHUTDOWN_TEMP = 90;
    public int PROTECTION_ALERT_TEMP = 70;


    //Discord bot (Leave out discord bot token if you don't want to use it)
    public String DISCORD_BOT_TOKEN = null;
    public String DISCORD_BOT_WHITELIST_USER_ID = "";
    public String DISCORD_BOT_SERVER_ID = "";
    public String DISCORD_BOT_CHANNEL_ID = "";
    public CustomCommand[] CUSTOM_COMMANDS = new CustomCommand[]{};


    //========================================================================================================================
    //========================================================================================================================
    //========================================================================================================================
    // === Internal stuff ===
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SETTINGS_PATH = Paths.get(System.getProperty("user.dir"), "settings.json");

    public static void save(AppSettings settings) {
        try (Writer writer = Files.newBufferedWriter(SETTINGS_PATH)) {
            GSON.toJson(settings, writer);
        } catch (Throwable e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    public static AppSettings load() {
        System.out.println("Loading settings from "+SETTINGS_PATH.toString());
        if (Files.exists(SETTINGS_PATH)) {
            try (Reader reader = Files.newBufferedReader(SETTINGS_PATH)) {
                return GSON.fromJson(reader, (Type) AppSettings.class);

            } catch (Throwable e) {
                System.err.println("Error loading settings: " + e.getMessage());
            }
        } else {
            save(new AppSettings()); // Save defaults if no file
        }
        return new AppSettings();
    }

}
