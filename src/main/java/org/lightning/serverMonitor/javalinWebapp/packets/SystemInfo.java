package org.lightning.serverMonitor.javalinWebapp.packets;

import org.lightning.serverMonitor.platform.FrequencyPolicy;

public class SystemInfo implements WsPacket {
    public String cpuVendor;
    public String governor;
    public FrequencyPolicy frequencyPolicy;
    public String powerState;
    public String ramUsage;


    public SystemInfo() {
    }
}
