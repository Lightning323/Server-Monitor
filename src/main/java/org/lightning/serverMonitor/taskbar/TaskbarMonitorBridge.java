package org.lightning.serverMonitor.taskbar;

import java.io.*;

class TaskbarMonitorBridge {

    private Process pythonProcess;
    private BufferedWriter pipeWriter;
    private String TASKBAR_FILE_PATH = "/python/taskbarText.py";

    public void startBridge() throws IOException {
        File scriptPath = extractResourceToTempFile();
        // Launch the Python appindicator helper script
        ProcessBuilder pb = new ProcessBuilder("python3", scriptPath.getAbsolutePath());
        pythonProcess = pb.start();

        // Attach a writer to Python's standard input stream
        pipeWriter = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream()));

        // Ensure the child process is terminated if the JVM shuts down
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopBridge));
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

    private File extractResourceToTempFile() throws IOException {
        InputStream resourceStream = getClass().getResourceAsStream(TASKBAR_FILE_PATH);
        if (resourceStream == null) {
            throw new IOException("Resource not found on classpath: " + TASKBAR_FILE_PATH);
        }

        // Create a temporary file in the OS temp directory
        File tempFile = File.createTempFile("taskbarText_", ".py");
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
            bridge.startBridge();

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