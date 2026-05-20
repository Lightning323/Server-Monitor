package org.pcMonitor.utils;

import org.pcMonitor.Main;

import javax.swing.*;

public class Logging {

    public static String errorString(Throwable e, boolean addMessage, boolean addStackTrace) {
        try {
            String exceptionType = e.getClass().getSimpleName();
            String message = "Error: " + exceptionType;

            if (addMessage && e.getMessage() != null) {
                message += " (" + e.getMessage() + ")";
            }

            if (addStackTrace) {
                message += "\n\nStack Trace:";
                for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                    stackTraceElement.toString();
                    message += "\n" + stackTraceElement.toString();
                }
            }
            return message;
        } catch (Exception ex) {
            return "Error: Unknown error";
        }
    }


    public static void error(Throwable e) {
        error(e, null);

    }

    public static void error(Throwable e, String title) {
        log(title,
                errorString(e, true, false),
                errorString(e, true, true),
                JOptionPane.ERROR_MESSAGE);
    }

    public static void log(String message) {
        log(null, message, message, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void log(String title,
                           String botMessage, String userMessage,
                           int messageType) {
        if (title == null) {
            title = switch (messageType) {
                case JOptionPane.INFORMATION_MESSAGE -> "Server Information";
                case JOptionPane.WARNING_MESSAGE -> "Server Warning";
                case JOptionPane.ERROR_MESSAGE -> "Server Error";
                default -> "Server Log";
            };
        }
        if (Main.bot != null && Main.bot.isReady()) {
            try {
                Main.bot.sendMessageToChannel(Main.settings.DISCORD_BOT_CHANNEL_ID,
                        "**" + title + "**\n" + botMessage);
            } catch (Exception botEx) {//If the bot fails, send a message to the user
                Logging.OS_dialog("Discord message error",
                        errorString(botEx, true, true),
                        JOptionPane.ERROR_MESSAGE, false);
            }
        }
        System.out.println("[LOG] " + userMessage + "\n");
    }

    public static void OS_dialog(String title, String message, int messageType, boolean blockThread) {
        System.out.println("[DIALOG] " + message + "\n");
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        try {
            if (blockThread) {
                JOptionPane.showMessageDialog(null, message, title, messageType);
            } else {
                new Thread(() -> {
                    JOptionPane.showMessageDialog(null, message, title, messageType);
                }).start();
            }
        } catch (Exception ex) {//JUST in case we have a problem with the dialog, show a simple message
            JOptionPane.showMessageDialog(null, "Unknown error", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


}
