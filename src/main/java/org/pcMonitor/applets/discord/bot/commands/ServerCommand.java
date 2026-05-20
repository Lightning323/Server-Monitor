package org.pcMonitor.applets.discord.bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.pcMonitor.applets.discord.bot.Command;

import java.io.IOException;

public class ServerCommand extends Command {
    public ServerCommand(String command) {
        super(command);
    }

    @Override
    public SlashCommandData register() {
        return null;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) throws IOException, InterruptedException {
//        OptionMapping commandArg = event.getOption("command");
//        String command = null;
//        if (commandArg != null) {
//                command = commandArg.getAsString();
//                System.out.println("command: " + command);
//                // Read the server's input first
//                Main.server.sendMessage(command);
//        }
//
//        int delay = 700;
//        OptionMapping delayArg = event.getOption("delay");
//        if (delayArg != null) {
//            delay = delayArg.getAsInt();
//            if (delay < 200) delay = 200;
//            else if (delay > 2000) delay = 2000;//Discord will have issues if the delay is too long
//        }
//
//        event.deferReply();
//        Thread.sleep(delay);
//        if (Main.server != null) {
//            int availableBytes = Main.server.getInputStream().available();
//            byte[] buffer = new byte[availableBytes];
//            Main.server.getInputStream().read(buffer);
//            String out = new String(buffer);
//            if (out.length() > 1500) {//Remove the last 1000 characters
//                out = ". . . " + out.substring(out.length() - 1500);
//            }
//            out = "```\n" + out + "\n```";
//            if (command != null) out = "Command: " + command + "\n" + out;
//            event.reply(out).setEphemeral(true).queue();
//        }
    }
}
