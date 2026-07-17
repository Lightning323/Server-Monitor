package org.lightning.serverMonitor;

import java.awt.*;
import java.net.URI;

import static org.lightning.serverMonitor.Main.LOGGER;

public class TaskbarTray {
    public static boolean canUseTray() {
        return SystemTray.isSupported();
    }

    boolean isStarted = false;

    public TaskbarTray() {
    }

    public void start() {
        if (canUseTray()) {
            if (isStarted) return;
            isStarted = true;
            try {
                // 1. Create the popup menu
                PopupMenu popup = new PopupMenu();

                // Create "Open Webapp" item
                MenuItem openItem = new MenuItem("Open Webapp");
                openItem.addActionListener(e -> {
                    try {
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(new URI("http://localhost:" + Main.config.WEBAPP_PORT));
                        } else {
                            LOGGER.warn("Desktop browsing is not supported on this system.");
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Failed to open webapp link", ex);
                    }
                });

                // Create "Exit" item
                MenuItem exitItem = new MenuItem("Exit");
                exitItem.addActionListener(e -> {
                    LOGGER.info("Exiting the application via tray icon.");
                    System.exit(0);
                });

                // Add items to the menu (with a nice separator between them)
                popup.add(openItem);
                popup.addSeparator();
                popup.add(exitItem);

                // 2. Load the icon safely
                var iconURL = TaskbarTray.class.getResource("/icon.png");
                if (iconURL == null) {
                    throw new NullPointerException("Could not find icon.png in resources!");
                }
                Image image = Toolkit.getDefaultToolkit().getImage(iconURL);

                // 3. Create the TrayIcon and attach the popup menu
                TrayIcon trayIcon = new TrayIcon(image, "Server Monitor");
                trayIcon.setImageAutoSize(true);
                trayIcon.setPopupMenu(popup);

                // 4. Add to the system tray
                SystemTray tray = SystemTray.getSystemTray();
                tray.add(trayIcon);

            } catch (Throwable t) {
                Main.LOGGER.error("Failed to start taskbar tray", t);
            }
        }
    }
}
