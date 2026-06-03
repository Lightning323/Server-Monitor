package org.lightning.serverMonitor.javalinWebapp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import org.lightning.serverMonitor.javalinWebapp.packets.WsPacket;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class JavalinWebApp {
    private final Javalin app;
    private final Set<WsContext> clients = ConcurrentHashMap.newKeySet();
    private final Gson gson = new Gson();
    private final HashMap<String, BiConsumer<String, WsContext>> packets = new HashMap<>();

    public JavalinWebApp() {
        app = Javalin.create(config -> {
            if (System.getProperty("env", "dev").equals("dev")) {
                // During development, point to the disk, NOT the JAR
                File f = new File("public");
                System.out.println(f.getAbsolutePath());
                config.staticFiles.add(f.getAbsolutePath().toString(), Location.EXTERNAL);
            } else {
                // In production, keep using resources
                config.staticFiles.add("/public", Location.CLASSPATH);
            }
        });
        app.ws("/ws", ws -> {
            ws.onConnect(this::onConnect);
            ws.onClose(this::onClose);
            ws.onMessage(this::onMessage);
        });
    }

    private void onMessage(WsMessageContext ctx) {
        String jsonString = ctx.message();
        JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
        String type = root.get("type").getAsString().toLowerCase().trim();
        String payload = root.get("payload").toString();
        if (packets.containsKey(type)) {
            packets.get(type).accept(payload, ctx);
        }
    }

    public void sendPacket(WsContext ctx, WsPacket packet) {
        JsonObject root = new JsonObject();
        root.addProperty("type", packet.getClass().getSimpleName());
        root.add("payload", gson.toJsonTree(packet));
        ctx.send(root.toString());
    }

    public <T extends WsPacket> void registerPacket(Class<T> clazz) {
        String type = clazz.getClass().getSimpleName();
        packets.put(type, (payload, ctx) -> {
            T packet = gson.fromJson(payload, clazz);
            packet.handle(ctx);
        });
    }

    protected void onConnect(WsConnectContext ctx) {
        clients.add(ctx);
    }

    private void onClose(WsCloseContext ctx) {
        clients.remove(ctx);
    }

    public void stop() {
        app.stop();
    }

    public void start(int port) {
        app.start(port);
    }

    public List<WsContext> getActiveClients() {
        return clients.stream()
                .filter(c -> c.session.isOpen())
                .toList();
    }
}
