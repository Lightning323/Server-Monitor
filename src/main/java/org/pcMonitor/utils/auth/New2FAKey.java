package org.pcMonitor.utils.auth;

import java.io.File;

import static org.pcMonitor.Main.APP_VERSION;

public class New2FAKey {

    public static void main(String[] args) {
        System.out.println("Version: " + APP_VERSION);
        String email = "sam.p.w524@gmail.com";

        try {
            String key = GoogleAuthenticator.generateSecretKey();
            String qrCodeUrl = GoogleAuthenticator.getQRCodeUrl(email, key, GoogleAuthenticator.ISSUER);
            System.out.println("Secret key: " + key);
            new File("temp").mkdirs();
            GoogleAuthenticator.generateQRCode(qrCodeUrl, "temp\\qrcode.png");
            while (true) {
                System.out.println("OTC (test): " + GoogleAuthenticator.getTOTP(key));
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
