package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.config.Config;

import java.util.Map;

public record SensorPropertiesPacket(Map<String, Config.SensorConfigProperty> sensors) implements WsPacket {
}
