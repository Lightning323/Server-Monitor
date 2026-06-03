package org.lightning.serverMonitor.javalinWebapp.packets;

import com.google.gson.JsonObject;
import io.javalin.websocket.WsContext;
import org.lightning.serverMonitor.javalinWebapp.JavalinWebApp;

public abstract interface WsPacket {

    public abstract void handle(WsContext ctx);
}
