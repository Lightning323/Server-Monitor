package org.pcMonitor;

import java.awt.*;

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

                // Create menu items for the popup menu
                MenuItem aboutItem = new MenuItem("About");
                MenuItem exitItem = new MenuItem("Exit");

                // Add menu items to the popup menu
                popup.add(aboutItem);
                popup.addSeparator(); // Adds a separator line
                popup.add(exitItem);

                // Create an action listener for the About menu item
                aboutItem.addActionListener(e -> System.out.println("This is a tray icon example."));

                // Create an action listener for the Exit menu item
                exitItem.addActionListener(e -> {
                    System.out.println("Exiting the application.");
                    System.exit(0);
                });

                // Load the icon from the resources directory
                Image image = Toolkit.getDefaultToolkit().getImage(
                        TaskbarTray.class.getResource("/icon.png") // Path to your resource file
                );

                // Create a tray icon
                TrayIcon trayIcon = new TrayIcon(image, "Tray Icon Example", popup);
                trayIcon.setImageAutoSize(true); // Automatically resize icon to fit the tray

                // Add a tooltip to the tray icon
                trayIcon.setToolTip("This is a Java Tray Icon example.");


                // Get the system tray and add the tray icon
                SystemTray tray = SystemTray.getSystemTray();
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.out.println("TrayIcon could not be added.");
                e.printStackTrace();
            }
        }
    }
}
