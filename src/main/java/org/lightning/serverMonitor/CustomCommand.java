package org.lightning.serverMonitor;

public class CustomCommand {
    public String command;
    public String identifier;

    public CustomCommand(String command, String description) {
        this.command = command;
        this.identifier = description;
    }

    public CustomCommand(){}
}
