package org.pcMonitor.applets.discord.bot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class DiscordCommandLogger {
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final File DISCORD_COMMAND_LOG = new File(System.getProperty("user.dir"), "discord-commands.log");

    public static File getLogFile() {
        return DISCORD_COMMAND_LOG;
    }

    public static void add(SlashCommandInteractionEvent event) {
        // Get the command name
        String commandName = event.getName();

        // Get all options as a formatted string
        String arguments = event.getOptions().stream()
                .map(option -> option.getName() + "=" + option.getAsString())
                .collect(Collectors.joining(" "));

        // Construct full command string
        String fullCommand = "/" + commandName + (arguments.isEmpty() ? "" : " " + arguments);

        // Print or use the full command string
        System.out.println("Command: " + fullCommand);

        //Get the timestamp
        String timestamp = LocalDateTime.now().format(formatter);

        try (FileWriter fw = new FileWriter(DISCORD_COMMAND_LOG, true)) { // 'true' enables append mode
            fw.write("\n" + timestamp + "  \t\t" + fullCommand);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
