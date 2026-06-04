//package org.lightning.serverMonitor.javalinWebapp.packets;
//
//import org.lightning.serverMonitor.Main;
//import org.lightning.serverMonitor.config.Config;
//import org.lightning.serverMonitor.javalinWebapp.PacketContext;
//
//import java.util.List;
//
//public record SensorsSelectedPacket(List<String> selected) implements WsPacket {
//
//    public void handle(PacketContext ctx) {
//        Main.config.SELECTED_SENSORS = selected;
//        Config.save(Main.config);
//    }
//}
