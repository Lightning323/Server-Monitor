package org.lightning.serverMonitor.applets.temprature;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

public class TempNetworkTool {
    private static final int PORT = 8888;
    private static final String BROADCAST_IP = "255.255.255.255";
    private static final int INTERVAL_MS = 2000;
    DatagramSocket socket;
    InetAddress address;

    public TempNetworkTool() {
        try {
            address = InetAddress.getByName(BROADCAST_IP);
            socket = new DatagramSocket();
            socket.setBroadcast(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void sendTemp(double temp) {
        try {
            String message = String.format("TEMP_REPORT:%.2fC", temp);
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, PORT);
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
