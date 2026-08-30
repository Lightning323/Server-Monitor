package org.lightning.serverMonitor.taskbar;

import org.lightning.serverMonitor.Main;
import org.lightning.serverMonitor.config.Config;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JOptionPane;

public class TaskbarTray {
    public static boolean canUseTray() {
        return SystemTray.isSupported();
    }

    final TaskbarMonitorBridge bridge;
    boolean isStarted = false;
    private final AtomicBoolean warningShown = new AtomicBoolean(false);

    public TaskbarTray() {
        bridge = new TaskbarMonitorBridge();
    }

    public void start() {
        if (canUseTray()) {
            if (isStarted) return;
            isStarted = true;
            try {
                bridge.startBridge(this::handleMenuAction, this::showTrayWarning);
            } catch (Throwable t) {
                isStarted = false;
                Main.LOGGER.error("Failed to start taskbar tray", t);
                showTrayWarning("The taskbar tray could not start. Install Python 3 and the GTK AppIndicator libraries.\n\nDetails: "
                        + t.getMessage());
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
            case "OPEN_CONFIG" -> {
                Main.LOGGER.notification("Open config requested from taskbar tray");
                Path configPath = Config.SETTINGS_PATH;
                try {
                    Desktop.getDesktop().open(configPath.toFile());
                } catch (Exception e) {
                    Main.LOGGER.error("Failed to open config file", e);
                }
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

    private void showTrayWarning(String message) {
        isStarted = false;
        if (!warningShown.compareAndSet(false, true)) {
            return;
        }

        Main.LOGGER.notification(message);
        if (!GraphicsEnvironment.isHeadless()) {
            EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    message,
                    "Server Monitor taskbar unavailable",
                    JOptionPane.WARNING_MESSAGE
            ));
        }
    }
}
