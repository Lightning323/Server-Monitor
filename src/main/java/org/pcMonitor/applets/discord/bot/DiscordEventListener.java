package org.pcMonitor.applets.discord.bot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.pcMonitor.Main;
import org.pcMonitor.applets.discord.bot.commands.HelloCommand;
import org.pcMonitor.applets.discord.bot.commands.InfoCommand;
import org.pcMonitor.applets.discord.bot.commands.ShutdownCommand;
import org.pcMonitor.applets.discord.bot.commands.SuspendCommand;
import org.pcMonitor.utils.Logging;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class DiscordEventListener extends ListenerAdapter {
    public DiscordBot bot;
    HashMap<String, Command> commands = new HashMap<>();

    public DiscordEventListener(DiscordBot bot) {
        this.bot = bot;
        commands.put("hello", new HelloCommand());
        commands.put("info", new InfoCommand());
        commands.put("shutdown", new ShutdownCommand());
        commands.put("suspend", new SuspendCommand());
    }

    // There is a reason why we don't add the commands IMMEDIENTLY after the bot starts up. The bot has to load in all the guilds it is in before it can add commands.
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println("Bot is ready!");
        bot.ready = true;
        registerGuildCommands(bot.getShardManager());
    }

    /**
     * This method is called when the bot is ready to add commands. This is where we add the commands to the server.
     */
    private void registerGuildCommands(ShardManager jda) {
        Guild g = jda.getGuildById(Main.settings.DISCORD_BOT_SERVER_ID); // Replace this with the ID of your own server.
        if (g != null) {
            CommandListUpdateAction commandRegistry = g.updateCommands();

            // Registering all commands in one action to prevent race conditions
            /**
             * RestRateLimiter WARN   Encountered 429 on route PUT/applications/{application_id}/guilds/{guild_id}/commands with bucket
             * can cause rate limits to be exceeded and commands to stop working
             */
            Command[] commandsList = commands.values().toArray(new Command[0]);
            SlashCommandData[] commandsToAdd = new SlashCommandData[commands.size()];
            for (int i = 0; i < commandsToAdd.length; i++) {
                commandsToAdd[i] = commandsList[i].register();
            }

            commandRegistry.addCommands(commandsToAdd).queue(
                    (success) -> {
                        System.out.println("Added " + success.size() + " commands");
                        success.forEach(s ->
                                System.out.println("Added command: " + s.getName()));
                    }
            );
        }
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        super.onShutdown(event);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        try {
            DiscordCommandLogger.add(event);
            Command command = commands.get(event.getName());
            if (command != null) {

                if (command.requiresMeToRun && !event.getUser().getId().equals(Main.settings.DISCORD_BOT_WHITELIST_USER_ID)) { //Onlu I can enter these commands
                    event.reply("You are not authorized to use this command!").setEphemeral(true).queue();
                } else {
                    command.onSlashCommandInteraction(event);
                }

            } else {
                event.reply("Command not found").setEphemeral(true).queue();
            }
        } catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e.getMessage());
        } catch (Exception e) {
            if (event.isAcknowledged()) Logging.error(e);
            else {
                event.reply(Logging.errorString(e, true, false)).setEphemeral(true).queue();
                System.out.println(Logging.errorString(e, true, true));
            }
        }
    }
}