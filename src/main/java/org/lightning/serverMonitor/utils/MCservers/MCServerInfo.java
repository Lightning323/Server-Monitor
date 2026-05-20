package org.lightning.serverMonitor.utils.MCservers;


public class MCServerInfo {
    public Version version;
    public Players players;
    public String description;
    public boolean enforcesSecureChat;

    protected MCServerInfo(Json_ServerInfo_1_20_1 info) {
        this.version = info.version;
        this.players = info.players;
        if (info.description != null) this.description = info.description.text;
        this.enforcesSecureChat = info.enforcesSecureChat;
    }

    public String toString() {
        return "ServerStatus{" +
                "version=" + version +
                ", players=" + players +
                ", description=" + description +
                ", enforcesSecureChat=" + enforcesSecureChat +
                '}';
    }
}
