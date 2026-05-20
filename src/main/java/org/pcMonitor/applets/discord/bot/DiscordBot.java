package org.pcMonitor.applets.discord.bot;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.FileUpload;
import org.pcMonitor.utils.Logging;

import javax.security.auth.login.LoginException;
import javax.swing.*;
import java.io.File;

public class DiscordBot {
    private ShardManager shardManager = null;
    protected boolean ready = false;

    public DiscordBot(String token) {
        try {
            shardManager = buildShardManager(token);
        } catch (LoginException e) {
            Logging.OS_dialog(
                    "Failed to start bot",
                    "Failed to start bot! Please check the console for any errors.",
                    JOptionPane.ERROR_MESSAGE, true);
            System.exit(1);
        }
    }

    public void waitUntilReady() throws InterruptedException {
        long startTime = System.currentTimeMillis(); // Get the current time in millisecont
        while (!isReady()) {
            if (System.currentTimeMillis() - startTime > 10000) {
                JOptionPane.showMessageDialog(null,
                        "Bot failed to start!", "Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            Thread.sleep(500);
        }
    }


    public void sendMessageToChannel(String channelId, String message) {
        if (ready) {
            // Fetch the shard and text channel
            TextChannel channel = shardManager.getTextChannelById(channelId);
            if (channel != null) {
                // Send a message to the channel
                channel.sendMessage(message).queue();
            } else {
                System.err.println("Channel not found!");
            }
        }
    }

    public void sendMessageWithFile(String channelId, String message, byte[] fileData, String fileName) {
        if (ready) {
            // Fetch the shard and text channel
            TextChannel channel = shardManager.getTextChannelById(channelId);
            if (channel != null) {
                // Create the file attachment
                FileUpload fileUpload = FileUpload.fromData(fileData, fileName);

                // Send the message with the file attachment
//            Message message = channel.sendMessage("Old content").addFiles(fileUpload).complete();
//            message.editMessage("New content").queue();

                channel.sendMessage(message).addFiles(fileUpload).queue();
            } else {
                System.out.println("Channel not found!");
            }
        }
    }

    public void sendMessageWithFile(String channelId, String message, File file) {
        if (ready) {
            // Fetch the shard and text channel
            TextChannel channel = shardManager.getTextChannelById(channelId);
            if (channel != null) {
                // Create the file attachment
                FileUpload fileUpload = FileUpload.fromData(file, file.getName());

                // Send the message with the file attachment
//            Message message = channel.sendMessage("Old content").addFiles(fileUpload).complete();
//            message.editMessage("New content").queue();

                channel.sendMessage(message).addFiles(fileUpload).queue();
            } else {
                System.out.println("Channel not found!");
            }
        }
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    public boolean isReady() {
        return ready;
    }

    private ShardManager buildShardManager(String token) throws LoginException {
        DefaultShardManagerBuilder builder =
                DefaultShardManagerBuilder.createDefault(token)
                        .addEventListeners(new DiscordEventListener(this));
        return builder.build();
    }

}
