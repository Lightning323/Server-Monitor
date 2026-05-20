package org.pcMonitor.platform;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;

public class CoreTempInterface {
    public final int serverPort;
    private String latestResponse = "";
    private SystemInfo latestSystemInfo = null;
    private boolean stopMode = false;
    Gson gson = new Gson();
    private Socket socket;
    Throwable fail = null; // used for checking if

    public CoreTempInterface(int serverPort) throws IOException {
        this.serverPort = serverPort;
        new Thread(this::run).start();

        //Wait for the CoreTempInterface to connect
        //If it doesn't connect within 10 seconds, exit
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (isConnected() || getFail() != null) break;
        }
        if (!isConnected() || getFail() != null) {
            throw new IOException("Failed to connect to CoreTempInterface");
        }
    }


    public void stop() {
        stopMode = true;
    }

    public SystemInfo getLatestSystemInfo() {
        return latestSystemInfo;
    }

    public boolean isConnected() {
        if (fail != null) return false;
        return latestSystemInfo != null && socket != null && socket.isConnected();
    }

    public Throwable getFail() {
        return fail;
    }

    private SystemInfo convertToClass(String json) {
        SystemInfo systemInfo = gson.fromJson(json, SystemInfo.class);
        return systemInfo;
    }

    private void run() {
        String serverAddress = "localhost";
        try {
            socket = new Socket(serverAddress, serverPort);
            System.out.println("Connected to server");
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                latestResponse = line;
                latestSystemInfo = convertToClass(line);
                dataReceived(latestResponse, latestSystemInfo);
                if (stopMode) {
                    socket.getInputStream().close();
                    break;
                }
            }

            System.out.println("Connection closed by server");
        } catch (Exception e) {
            e.printStackTrace();
            fail = e;
        }
    }

    private void dataReceived(String latestResponse, SystemInfo latestSystemInfo) {
        System.out.println("Received: " + latestResponse);
    }

}
