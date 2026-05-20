//package org.pcMonitor.javalinWebapp;
//
//import io.javalin.Javalin;
//import io.javalin.websocket.WsContext;
//
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
//public class Example {
//    public static void main(String[] args) {
//        Set<WsContext> clients = ConcurrentHashMap.newKeySet();
//        AtomicInteger counter = new AtomicInteger();
//
//        Javalin app = Javalin.create(config -> {
//            config.staticFiles.add("/example"); // serves /public/index.html
//        }).start();
//
//        app.ws("/ws", ws -> {
//            ws.onConnect(ctx -> {
//                clients.add(ctx);
//                System.out.println("Connected: ");
//            });
//            ws.onClose(ctx -> {
//                clients.remove(ctx);
//                System.out.println("Disconnected: ");
//            });
//        });
//
//        // Schedule counter updates every second
//        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
//            int value = counter.incrementAndGet();
//            clients.forEach(session -> {
//                session.send(String.valueOf(value));
//            });
//        }, 0, 1, TimeUnit.SECONDS);
//    }
//}
