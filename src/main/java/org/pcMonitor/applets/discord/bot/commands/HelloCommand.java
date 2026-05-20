package org.pcMonitor.applets.discord.bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.pcMonitor.Main;
import org.pcMonitor.applets.discord.bot.Command;

public class HelloCommand extends Command {
    public HelloCommand() {
        super("hello");
        requiresMeToRun = false;
    }

    @Override
    public SlashCommandData register() {
        return Commands.slash(name, "Have the bot say hello to you in an ephemeral message!");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String message = "Hello " + event.getUser().getAsMention() + "!  (app V" + Main.APP_VERSION + ")";
        event.reply(message).setEphemeral(true).queue();
    }
}
