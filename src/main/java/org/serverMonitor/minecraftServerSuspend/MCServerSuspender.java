//package org.serverMonitor.minecraftServerSuspend;
//
//import org.pcMonitor.Main;
//import org.pcMonitor.utils.Logging;
//import org.pcMonitor.utils.MCservers.MCServerChecker;
//import org.pcMonitor.utils.MCservers.MCServerInfo;
//
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//public class MCServerSuspender {
//
//    public static void start() {
//        //Start the suspend tracker
//        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
//            try {
//                int allPlayersOnline = 0;
//                int activeServers = 0;
//                for (int port : Main.settings.MINECRAFT_SERVER_PORTS) {
//                    MCServerInfo server = MCServerChecker.getMCServerInfo("localhost", port, 1000);
//                    if (server != null) {
//                        activeServers++;
//                        System.out.println(server.toString());
//                        allPlayersOnline += server.players.online;
//                        if (allPlayersOnline > 0) break;
//                    }
//                }
//
//                long millis = Main.platform.getAwakeMillis();
//                if (millis > -1) {
//                    Main.minutesSinceAwake = millis / 1000 / 60;
////                    if (
////                            activeServers > 0 &&
////                                    allPlayersOnline == 0 &&
////                                    Main.minutesSinceAwake > Main.settings.INACTIVITY_SHUTDOWN_MINUTES &&
////                                    Main.settings.INACTIVITY_SHUTDOWN_MINUTES > 0 &&
////                                    Main.platform.isUserInactive()) {
////                        System.out.println("Wanting to suspend...");
////                        Main.platform.suspend(Main.minutesSinceAwake + " min of MC server inactivity");
////                    }
//                } else Main.minutesSinceAwake = -1;
//
//            } catch (Throwable t) {
//                Logging.error(t);
//            }
//        }, 0, 1, TimeUnit.MINUTES);
//    }
//}
