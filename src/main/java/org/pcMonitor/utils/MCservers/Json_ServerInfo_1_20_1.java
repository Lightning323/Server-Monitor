package org.pcMonitor.utils.MCservers;

class Json_ServerInfo_1_20_1 {
    Version version;
    Players players;
    Description description;
    boolean enforcesSecureChat;

    public String toString() {
        return "ServerStatus{" +
                "version=" + version +
                ", players=" + players +
                ", description=" + description +
                ", enforcesSecureChat=" + enforcesSecureChat +
                '}';
    }
}