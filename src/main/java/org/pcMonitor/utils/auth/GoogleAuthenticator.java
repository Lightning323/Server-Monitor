package org.pcMonitor.utils.auth;

import org.apache.commons.codec.binary.Base32;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Scanner;

public class GoogleAuthenticator {

    public static final String ISSUER = "NexusDiscordApp"; // Name of your app

    //Example code
    public static void main(String[] args) throws Exception {
        // Generate a secret key
        String secretKey = generateSecretKey();
        System.out.println("Secret: " + secretKey);

//        // Generate a QR Code
        String qrCodeUrl = getQRCodeUrl("sam.p.w524@gmail.com", secretKey, ISSUER);
        generateQRCode(qrCodeUrl, "qrcode.png");
        System.out.println("QR Code generated: " + qrCodeUrl);


        while (true) {
            // Simulate user entering an OTP
            int otp = getTOTP(secretKey);
            System.out.println("Generated OTP: " + otp);

            //Ask user to enter the OTP
            Scanner scanner = new Scanner(System.in);
            String userOTP = scanner.nextLine();
            int enteredOTP = Integer.parseInt(userOTP);

            // Verify the OTP
            boolean isValid = verifyOTP(secretKey, enteredOTP);
            System.out.println("OTP is valid: " + isValid);
        }

    }

    public static String generateSecretKey() {
        Base32 base32 = new Base32();
        byte[] secretBytes = new byte[10];
        for (int i = 0; i < secretBytes.length; i++) {
            secretBytes[i] = (byte) (Math.random() * 256);
        }
        return base32.encodeToString(secretBytes);
    }

    public static String getQRCodeUrl(String email, String secret, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer, email, secret, issuer);
    }

    public static void generateQRCode(String data, String filePath) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200);
        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }

    public static int getTOTP(String secret) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        long timeIndex = Instant.now().getEpochSecond() / 30; // Time step of 30 seconds
        byte[] key = new Base32().decode(secret);
        byte[] timeBytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            timeBytes[i] = (byte) (timeIndex & 0xFF);
            timeIndex >>= 8;
        }
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        mac.init(signKey);
        byte[] hmac = mac.doFinal(timeBytes);

        // Extract the dynamic binary code
        int offset = hmac[hmac.length - 1] & 0x0F;
        int otp = ((hmac[offset] & 0x7F) << 24)
                | ((hmac[offset + 1] & 0xFF) << 16)
                | ((hmac[offset + 2] & 0xFF) << 8)
                | (hmac[offset + 3] & 0xFF);
        return otp % 1000000; // Return the OTP as a 6-digit number
    }

    public static boolean verifyOTP(String secretKey, int otp) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        int generatedOtp = getTOTP(secretKey);
        return generatedOtp == otp;
    }
}
