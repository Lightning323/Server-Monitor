package org.lightning.serverMonitor.taskbar;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

class TaskbarMonitorBridge {

    private Process pythonProcess;
    private BufferedWriter pipeWriter;
    private static final String TASKBAR_FILE_PATH = "/python/taskbarText.py";
    private static final String ICON_FILE_PATH = "/icon_large.png";

    public void startBridge(Consumer<String> menuActionHandler) throws IOException {
        File scriptPath = extractResourceToTempFile(TASKBAR_FILE_PATH, ".py");
        File iconPath = extractResourceToTempFile(ICON_FILE_PATH, ".png");
        // Launch the Python appindicator helper script
        ProcessBuilder pb = new ProcessBuilder("python3", scriptPath.getAbsolutePath(), iconPath.getAbsolutePath());
        pythonProcess = pb.start();

        // Attach a writer to Python's standard input stream
        pipeWriter = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream()));

        Thread menuActionReader = new Thread(() -> readMenuActions(menuActionHandler), "taskbar-menu-action-reader");
        menuActionReader.setDaemon(true);
        menuActionReader.start();

        // Ensure the child process is terminated if the JVM shuts down
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopBridge));
    }

    private void readMenuActions(Consumer<String> menuActionHandler) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(pythonProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String action;
            while ((action = reader.readLine()) != null) {
                menuActionHandler.accept(action.trim());
            }
        } catch (IOException e) {
            if (pythonProcess != null && pythonProcess.isAlive()) {
                System.err.println("Failed to read taskbar menu action: " + e.getMessage());
            }
        }
    }

    public void updateTaskbarText(String text) {
        if (pipeWriter == null) {
            System.err.println("Bridge is not running. Call startBridge() first.");
            return;
        }

        try {
            pipeWriter.write(text);
            pipeWriter.newLine(); // Python's sys.stdin expects a line break
            pipeWriter.flush();   // Flush buffer immediately to update the panel UI
        } catch (IOException e) {
            System.err.println("Failed to write to taskbar process pipe: " + e.getMessage());
        }
    }

    public void stopBridge() {
        try {
            if (pipeWriter != null) {
                pipeWriter.close();
            }
        } catch (IOException ignored) {}

        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroyForcibly();
        }
    }

    private File extractResourceToTempFile(String resourcePath, String suffix) throws IOException {
        InputStream resourceStream = getClass().getResourceAsStream(resourcePath);
        if (resourceStream == null) {
            throw new IOException("Resource not found on classpath: " + resourcePath);
        }

        // Create a temporary file in the OS temp directory
        File tempFile = File.createTempFile("server-monitor-tray_", suffix);
        tempFile.deleteOnExit(); // Cleanup fallback

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = resourceStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        return tempFile;
    }

    public static void main(String[] args) {
        TaskbarMonitorBridge bridge = new TaskbarMonitorBridge();

        try {
            bridge.startBridge(action -> System.out.println("Tray action: " + action));

            // Simulation loop: Update the taskbar every 2 seconds
            int mockGpuTemp = 38;
            while (true) {
                String telemetry = String.format("3%% %d°C 0RPM 34°C 40°C 46°C", mockGpuTemp);
                bridge.updateTaskbarText(telemetry);

                mockGpuTemp = (mockGpuTemp >= 60) ? 38 : mockGpuTemp + 1;
                Thread.sleep(2000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
