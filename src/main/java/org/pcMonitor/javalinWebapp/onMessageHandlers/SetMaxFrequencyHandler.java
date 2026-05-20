package org.pcMonitor.javalinWebapp.onMessageHandlers;

import io.javalin.websocket.WsMessageContext;
import org.pcMonitor.javalinWebapp.JavalinWebApp;
import org.pcMonitor.javalinWebapp.OnMessageHandlers;
import org.pcMonitor.platform.FrequencyPolicy;
import org.pcMonitor.platform.Platform;

import static org.pcMonitor.javalinWebapp.JavalinWebApp.DELIMITER;

public class SetMaxFrequencyHandler extends OnMessageHandlers {

    public SetMaxFrequencyHandler(JavalinWebApp app) {
        super("set-max-frequency", app);
    }

    public void onMessage(WsMessageContext ctx, String message) {
        double maxFreqMHZ = Double.parseDouble(message.split(DELIMITER)[1]);
        if (maxFreqMHZ >= 0) {//Only set if valid (Sometimes the client sends 0 just to get the current policy)
            FrequencyPolicy frequencyPolicy = Platform.SINGLETON.setMaxFrequencyMHZ(maxFreqMHZ);
            app.sendFrequencyPolicy(ctx, frequencyPolicy);
        } else {
            app.sendFrequencyPolicy(ctx, Platform.SINGLETON.getFrequencyPolicy());
        }
    }

}
