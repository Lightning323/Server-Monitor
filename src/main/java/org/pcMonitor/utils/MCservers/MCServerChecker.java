package org.pcMonitor.utils.MCservers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MCServerChecker {

    private static final Gson gson = new Gson();

    public static MCServerInfo getMCServerInfo(String hostname, int port, int timeoutMS) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostname, port), timeoutMS);
            try {
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                ByteArrayOutputStream handshake = new ByteArrayOutputStream();

                PacketUtils.writeVarInt(handshake, 0x00); // packet id for handshake
                PacketUtils.writeVarInt(handshake, 754); // protocol version (754 = 1.16.5+)
                PacketUtils.writeVarInt(handshake, hostname.length());
                handshake.write(hostname.getBytes(StandardCharsets.UTF_8));
                handshake.write((port >> 8) & 0xFF);
                handshake.write(port & 0xFF);
                PacketUtils.writeVarInt(handshake, 1); // next state: status

                PacketUtils.writeVarInt(out, handshake.size());
                out.write(handshake.toByteArray());

                out.write(1); // size of next packet
                out.write(0); // request status

                int size = PacketUtils.readVarInt(in);
                int id = PacketUtils.readVarInt(in);
                if (id != 0x00) throw new IOException("Invalid packet ID");

                int length = PacketUtils.readVarInt(in);
                byte[] data = new byte[length];

                int bytesRead = 0;
                while (bytesRead < length) {
                    int result = in.read(data, bytesRead, length - bytesRead);
                    if (result == -1) throw new IOException("Unexpected end of stream");
                    bytesRead += result;
                }


                String json = new String(data, StandardCharsets.UTF_8);
                JsonReader reader = new JsonReader(new StringReader(json));
                reader.setStrictness(Strictness.LENIENT);
                JsonElement root = JsonParser.parseString(json);

                if (root.getAsJsonObject().get("description").isJsonPrimitive()) {
                    // likely 1.21.x
                    return gson.fromJson(root, MCServerInfo.class);
                } else {
                    // likely 1.20.x fancy object
                    Json_ServerInfo_1_20_1 info = gson.fromJson(root, Json_ServerInfo_1_20_1.class);
                    return new MCServerInfo(info);
                }

            } catch (Throwable e) {
                System.out.println("Unable to retrieve server info");
                if (e.getStackTrace() != null) {
                    for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                        System.out.println(stackTraceElement.toString());
                    }
                }
                return null; // Invalid response
            }
        } catch (IOException e) {
            return null; // Couldn't connect
        }
    }

}