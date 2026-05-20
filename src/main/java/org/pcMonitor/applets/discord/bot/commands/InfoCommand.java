package org.pcMonitor.applets.discord.bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.pcMonitor.Main;
import org.pcMonitor.applets.discord.bot.Command;
import org.pcMonitor.platform.Platform;

public class InfoCommand extends Command {
    public InfoCommand() {
        super("info");
    }

    @Override
    public SlashCommandData register() {
        return Commands.slash(name, "Info about the computer and server");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.reply(
                "**CPU temp: " + Platform.SINGLETON.getCPUTempCelsius() + "C**" +
                        "\n**CPU load: " + Platform.SINGLETON.getImmediateCPULoad() + "%**" +
                        "\n" + Platform.SINGLETON.getFrequencyPolicy().toString() +
                        "\nMemory: " + Platform.SINGLETON.getOSRamUsage() +
                        "\n" +
                        "\nTime over temp: " +
                        "\n```" + Main.tempMonitor.getTimeOverTemp() + "```" +
                        "\n\n\n" +
                        "\nLoad info: " + Platform.SINGLETON.getCPULoadStr() +
                        "\nRunning as admin: " + Platform.IS_ADMIN +
                        "\nRuntime MS: " + Platform.SINGLETON.getAwakeMillis()).setEphemeral(true).queue();
    }
}
