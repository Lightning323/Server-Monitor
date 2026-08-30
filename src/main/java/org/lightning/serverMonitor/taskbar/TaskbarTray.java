package org.lightning.serverMonitor.taskbar;

import org.lightning.serverMonitor.Main;

import java.awt.*;
import java.net.URI;

import static org.lightning.serverMonitor.Main.LOGGER;

public class TaskbarTray {
    public static boolean canUseTray() {
        return SystemTray.isSupported();
    }

    final TaskbarMonitorBridge bridge;
    boolean isStarted = false;

    public TaskbarTray() {
        bridge = new TaskbarMonitorBridge();
    }

    public void start() {
        if (canUseTray()) {
            if (isStarted) return;
            isStarted = true;
            try {
                bridge.startBridge();
            } catch (Throwable t) {
                Main.LOGGER.error("Failed to start taskbar tray", t);
            }
        }
    }

    public void update(String str) {
        bridge.updateTaskbarText(str);
    }

    public boolean isEnabled() {
        return isStarted;
    }
}
