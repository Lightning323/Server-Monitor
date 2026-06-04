package org.lightning.serverMonitor.javalinWebapp.packets;

import java.util.HashMap;

public record SensorAliasesPacket(HashMap<String, String> aliases) implements WsPacket {
}
