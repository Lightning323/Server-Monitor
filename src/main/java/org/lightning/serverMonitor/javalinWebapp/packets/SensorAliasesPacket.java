package org.lightning.serverMonitor.javalinWebapp.packets;

import java.util.HashMap;
import java.util.Map;

public record SensorAliasesPacket(Map<String, String> aliases) implements WsPacket {
}
