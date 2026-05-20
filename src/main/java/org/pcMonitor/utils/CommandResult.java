package org.pcMonitor.utils;

public class CommandResult {
    public final int exitCode;
    public final String stdout;
    public final String stderr;

    public CommandResult(int exitCode, String string, String string1) {
        this.exitCode = exitCode;
        this.stdout = string;
        this.stderr = string1;
    }
}
