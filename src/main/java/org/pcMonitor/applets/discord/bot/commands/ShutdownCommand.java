package org.pcMonitor.applets.discord.bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.pcMonitor.applets.discord.bot.Command;
import org.pcMonitor.platform.Platform;

public class ShutdownCommand extends Command {
    public ShutdownCommand() {
        super("shutdown");
    }

    @Override
    public SlashCommandData register() {
        return Commands.slash(name, "Shutdown the server");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.reply("Shutting down").setEphemeral(true).queue();
        Platform.SINGLETON.shutdown(null);
    }
}
