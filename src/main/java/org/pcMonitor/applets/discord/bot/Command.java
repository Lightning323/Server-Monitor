package org.pcMonitor.applets.discord.bot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public abstract class Command {
    public final String name;
    public boolean requiresMeToRun = true;

    public Command(String command) {
        this.name = command;
    }

    public abstract SlashCommandData register();

    public abstract void onSlashCommandInteraction(SlashCommandInteractionEvent event) throws Exception;
}
