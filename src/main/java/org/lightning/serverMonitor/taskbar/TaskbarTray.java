package org.lightning.serverMonitor.taskbar;

import org.lightning.serverMonitor.Main;

import java.awt.*;
import java.net.URI;

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
                bridge.startBridge(this::handleMenuAction);
            } catch (Throwable t) {
                isStarted = false;
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

    private void handleMenuAction(String action) {
        switch (action) {
            case "OPEN_WEBAPP" -> openWebApp();
            case "QUIT" -> {
                Main.LOGGER.notification("Quit requested from taskbar tray");
                System.exit(0);
            }
            default -> Main.LOGGER.debug("Ignoring unknown taskbar menu action: " + action);
        }
    }

    private void openWebApp() {
        String webAppUrl = "http://localhost:" + Main.config.WEBAPP_PORT;
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Main.LOGGER.notification("Cannot open web app automatically; open " + webAppUrl + " manually.");
                return;
            }
            Desktop.getDesktop().browse(URI.create(webAppUrl));
        } catch (Exception e) {
            Main.LOGGER.error("Failed to open web app at " + webAppUrl, e);
        }
    }
}
