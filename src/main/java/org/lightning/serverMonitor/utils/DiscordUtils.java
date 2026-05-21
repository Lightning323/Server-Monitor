package org.lightning.serverMonitor.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DiscordUtils {
    public static boolean sendWebhookMessage(String webhookUrl, String message) {
        String jsonPayload = "{\"content\": \"" + message
                .replaceAll("\"", "")
                .replaceAll("\n", "\\n")
                .replaceAll("\r","\\n") + "\"}";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        try {
            client.send(request, HttpResponse.BodyHandlers.ofString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
