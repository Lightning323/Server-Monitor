package org.pcMonitor.utils.MCservers;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PacketUtils {

    // Helper methods
    public static void sendPacket(DataOutputStream out, byte[] data) throws IOException {
        writeVarInt(out, data.length);
        out.write(data);
        out.flush();
    }

    public static int readVarInt(InputStream in) throws IOException {
        int numRead = 0, result = 0;
        byte read;
        do {
            read = (byte) in.read();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) throw new IOException("VarInt too big");
        } while ((read & 0b10000000) != 0);
        return result;
    }

    public static void writeVarInt(OutputStream out, int value) throws IOException {
        do {
            byte temp = (byte) (value & 0b01111111);
            value >>>= 7;
            if (value != 0) temp |= 0b10000000;
            out.write(temp);
        } while (value != 0);
    }

    public static String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeString(DataOutputStream out, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }
}
