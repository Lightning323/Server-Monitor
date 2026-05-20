package org.lightning.serverMonitor;

import java.awt.*;

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
                // Create a popup menu
                PopupMenu popup = new PopupMenu();
                MenuItem exitItem = new MenuItem("Exit");

                popup.addSeparator(); // Adds a separator line
                popup.add(exitItem);

                // Create an action listener for the Exit menu item
                exitItem.addActionListener(e -> {
                    System.out.println("Exiting the application.");
                    System.exit(0);
                });

                // Load the icon from the resources directory
                TrayIcon trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(
                        TaskbarTray.class.getResource("/icon.png") // Path to your resource file
                ));
                trayIcon.setImageAutoSize(true); // Automatically resize icon to fit the tray

                SystemTray tray = SystemTray.getSystemTray();
                tray.add(trayIcon);
            } catch (Throwable t) {
                Main.LOGGER.error("Failed to start taskbar tray",t);
            }
        }
    }
}
