package org.lightning.serverMonitor.javalinWebapp;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import org.lightning.serverMonitor.CustomCommand;
import org.lightning.serverMonitor.Main;
import org.lightning.serverMonitor.javalinWebapp.onMessageHandlers.LoadHistoryHandler;
import org.lightning.serverMonitor.javalinWebapp.onMessageHandlers.SetMaxFrequencyHandler;
import org.lightning.serverMonitor.javalinWebapp.onMessageHandlers.ShutdownHandler;
import org.lightning.serverMonitor.logging.HistoryRecord;
import org.lightning.serverMonitor.platform.FrequencyPolicy;
import org.lightning.serverMonitor.platform.Platform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JavalinWebApp {
    public static final String DELIMITER = "###";
    public static final String ALERT_HEADER = "alert" + DELIMITER;
    public static final String LIVE_DATA_HEADER = "live-data" + DELIMITER;
    public static final int LIVE_DATA_POINTS = 200;


    Set<WsContext> clients = ConcurrentHashMap.newKeySet();
    WebappHistory history = new WebappHistory();

    List<OnMessageHandlers> onMessageHandlers = new ArrayList<>();

    public JavalinWebApp() {
        onMessageHandlers.add(new LoadHistoryHandler(this));
        onMessageHandlers.add(new SetMaxFrequencyHandler(this));
        onMessageHandlers.add(new ShutdownHandler(this));
    }

    public void addDataPoint(double cpuTemp, double cpuLoad) {
        HistoryRecord historyRecord = history.addRecord(cpuTemp, cpuLoad);
        clients.forEach(session -> {
            session.send(historyRecord.toString(LIVE_DATA_HEADER));
        });

    }

    public void sendHistory(WsMessageContext ctx) {
        ctx.send(history.getRecordsAsString("history" + DELIMITER));
    }

    public void sendHistory(WsMessageContext ctx, String filename) {
        ctx.send(history.getRecordsAsString("history" + DELIMITER, filename));
    }

    public void start() {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public"); // serves /public/index.html
        }).start(Main.settings.WEBAPP_LOCALHOST_PORT);
        System.out.println("\n\n\nStarted webapp on port " + Main.settings.WEBAPP_LOCALHOST_PORT + "\n\n\n");

        app.ws("/ws", ws -> {

                    ws.onConnect(ctx -> {

                        System.out.println("Connected to client: " + ctx.toString());
                        ctx.send("app-version" + DELIMITER + Main.APP_VERSION);
                        ctx.send("server-name" + DELIMITER + Main.settings.SERVER_NAME);
                        clients.add(ctx);
                        ctx.send("is-admin" + DELIMITER + Platform.IS_ADMIN);

                        ctx.send("time-since-awake"
                                + DELIMITER + Main.minutesSinceAwake
                                + " min  (" + (Platform.SINGLETON.isUserInactive() ? "inactive" : "active") + ")"
                        );

                        ctx.send("app-interval" + DELIMITER + Main.settings.SENSORS_UPDATE_MS);
                        sendFrequencyPolicy(ctx, Platform.SINGLETON.getFrequencyPolicy());
                        ctx.send("history-records" + DELIMITER + getSortedHistoryRecords());

                        ctx.send("clear-charts" + DELIMITER);
                        int l = Math.min(LIVE_DATA_POINTS, history.size());
                        int start = history.size() - l;
                        int end = history.size();
                        int step = end - start > 100 ? 2 : 1;
//                        System.out.println("start: " + start + " end: " + end + " step: " + step);
                        String liveData = history.getRecordsAsString(LIVE_DATA_HEADER, start, end, step);
                        ctx.send(liveData);
                        ctx.send("ram" + DELIMITER + Platform.SINGLETON.getOSRamUsage());
                        ctx.send("cpu-temp-history" + DELIMITER + Main.tempMonitor.getTimeOverTemp());
                    });
                    ws.onClose(ctx -> {
                        System.out.println("Disconnected: " + ctx.toString());
                        clients.remove(ctx);
                    });
                    ws.onMessage(ctx -> {
                        try {
                            String msg = ctx.message();
                            System.out.println("Received from client: " + msg);
                            for (OnMessageHandlers handler : onMessageHandlers) {
                                if (
                                        (msg.equals(handler.prefix) && msg.startsWith(handler.prefix)) //If the message is the prefix
                                                || msg.startsWith(handler.prefix + DELIMITER) //If the message starts with the prefix
                                ) {
                                    handler.onMessage(ctx, msg);
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            ctx.send("alert" + DELIMITER + "Error: " + e.getMessage());
                        }
                    });
                }
        );
    }

    private String getSortedHistoryRecords() {
        File[] historyRecords = new File(WebappHistory.TEMP_HISTORY_DIR).listFiles();
        StringBuilder historyStr = new StringBuilder();

        if (historyRecords != null && historyRecords.length > 0) {
            //Sort them according to last modified
            java.util.Arrays.sort(historyRecords, (f1, f2) -> {
                return Long.compare(f2.lastModified(), f1.lastModified());
            });
            for (File record : historyRecords) {
                //System.out.println(record.getName());
                historyStr.append(record.getName()).append(",");
            }
        }
        return historyStr.toString();
    }

    public void sendFrequencyPolicy(WsContext ctx, FrequencyPolicy policy) {
        ctx.send("frequency-policy" + DELIMITER + policy.toString() + "\n\n\n" + policy.rawCommandOutput);
        ctx.send("min-software-frequency" + DELIMITER + policy.minSoftwareFrequencyMHZ);
        ctx.send("max-software-frequency" + DELIMITER + policy.maxSoftwareFrequencyMHZ);
        ctx.send("min-hardware-frequency" + DELIMITER + policy.minHardwareFrequencyMHZ);
        ctx.send("max-hardware-frequency" + DELIMITER + policy.maxHardwareFrequencyMHZ);
    }

}
