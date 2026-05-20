//package org.example.bot;
//
//import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
//import net.dv8tion.jda.api.utils.FileUpload;
//import org.example.Main;
//import org.example.utils.Logging;
//
//import javax.swing.*;
//
//public class AuthRequiredCommands {
//    private static long verificationTimestamp;
//    private static int wrongKeyAttempts = 0;
//    private static long lockoutTimestamp;
//
//    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    private static final int MAX_KEY_ATTEMPTS = 3;
//    public static final long LOCKOUT_TIME = 1000 * 60 * 25;
//    private static final long VERIFICATION_TIMEOUT = 1000 * 60 * 5;
//    public static boolean ALWAYS_ALLOW = false;
//    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//    // <editor-fold desc="Authentication" collapsed="true">
//
//    public static void deVerify() {
//        verificationTimestamp = 0;
//    }
//
//    public static void lockout() {
//        verificationTimestamp = 0;
//        lockoutTimestamp = System.currentTimeMillis();
//    }
//
//    public static boolean isLockedOut() {
//        return System.currentTimeMillis() - lockoutTimestamp < LOCKOUT_TIME;
//    }
//
//    public static boolean isVerified() {
//        if (ALWAYS_ALLOW) return true;
//        if (isLockedOut()) return false;
//        return System.currentTimeMillis() - verificationTimestamp < VERIFICATION_TIMEOUT;
//    }
//
//    private static boolean verify2FA(SlashCommandInteractionEvent event) {
//        boolean access = false;
//
//        if (isLockedOut()) {
//            long timeLeftMillis = LOCKOUT_TIME - (System.currentTimeMillis() - lockoutTimestamp);
//            long minutesLeft = Math.max(0, timeLeftMillis / 1000 / 60); // Avoid negative time
//            event.reply("You have been temporarily locked out. Wait " +
//                    minutesLeft + " minutes before trying again.").queue();
//            return false;
//        }
//
//        //If we already verified the user in the last 2 minutes, we don't need to do it again
//        if (isVerified()) {
//            access = true;
//        } else {
//            if (event.getOption("code") == null) {
//                event.reply("Verification expired. Please enter valid code").setEphemeral(true).queue();
//                return false;
//            }
//            //Authentication required commands
//            int otpKey = (int) event.getOption("code").getAsInt();
//            try {
//                access = Main.twoFactorAuth(otpKey);
//                if (access) verificationTimestamp = System.currentTimeMillis();
//            } catch (Exception e) {
//                event.reply("Invalid key (Error processing key)").setEphemeral(true).queue();
//                return false;
//            }
//        }
//
//        return access;
//    }
//    // </editor-fold>
//
//    /**
//     *
//     * @param event
//     * @return if the event was consumed
//     */
//    public static boolean runAuthCommands(SlashCommandInteractionEvent event) {
//        try {
//            if (ALWAYS_ALLOW || verify2FA(event)) {
//                wrongKeyAttempts = 0;
//                /////////////////////////////////////////////////////////////////////////////////////////////////////
//                /////////////////////////////////////////////////////////////////////////////////////////////////////
//
//                if (event.getName().equals("verify")) {
//                    event.reply("Verification successful").setEphemeral(true).queue();
//                    return true;
//                } else if (event.getName().equals("log")) {
//                    FileUpload file = FileUpload.fromData(Main.DISCORD_COMMAND_LOG, "command_log.txt");
//                    event.reply("Here is the log").addFiles(file).setEphemeral(true).queue();
//                    return true;
//                }
//
//                /////////////////////////////////////////////////////////////////////////////////////////////////////
//                /////////////////////////////////////////////////////////////////////////////////////////////////////
//            } else {
//                wrongKeyAttempts++;
//                if (wrongKeyAttempts > MAX_KEY_ATTEMPTS) {
//                    event.reply("Wrong key! You have been locked out for " + (int) (LOCKOUT_TIME / 1000 / 60) + " minutes").queue();
//                    lockout();
//                } else event.reply("Wrong key!").setEphemeral(true).queue();
//                return true;
//            }
//        } catch (IllegalStateException e) {
//            System.out.println("IllegalStateException: " + e.getMessage());
//        } catch (Exception e) {
//            if (event.isAcknowledged()) {
//                Logging.log(null, //We want minimum details so we cant get hacked
//                        Logging.errorString(e, isVerified(), false),
//                        Logging.errorString(e, true, true),
//                        JOptionPane.ERROR_MESSAGE, false, false);
//            } else {
//                event.reply(Logging.errorString(e, isVerified(), false)).setEphemeral(true).queue();
//                System.out.println(Logging.errorString(e, true, true));
//            }
//            return true;
//        }
//        return false;
//    }
//}
