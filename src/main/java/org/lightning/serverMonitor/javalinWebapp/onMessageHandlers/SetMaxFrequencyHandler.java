package org.lightning.serverMonitor.javalinWebapp.onMessageHandlers;

import io.javalin.websocket.WsMessageContext;
import org.lightning.serverMonitor.javalinWebapp.JavalinWebApp;
import org.lightning.serverMonitor.javalinWebapp.OnMessageHandlers;
import org.lightning.serverMonitor.platform.FrequencyPolicy;
import org.lightning.serverMonitor.platform.Platform;

import static org.lightning.serverMonitor.javalinWebapp.JavalinWebApp.DELIMITER;

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
