package org.pcMonitor.applets.discord.bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.pcMonitor.applets.discord.bot.Command;
import org.pcMonitor.platform.Platform;

public class SuspendCommand extends Command {
    public SuspendCommand() {
        super("suspend");
    }

    @Override
    public SlashCommandData register() {
        return Commands.slash(name, "Suspend the server");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.reply("Suspending...").setEphemeral(true).queue();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        Platform.SINGLETON.suspend(null);
    }
}
