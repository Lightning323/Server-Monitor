package org.lightning.serverMonitor.javalinWebapp.onMessageHandlers;

import io.javalin.websocket.WsMessageContext;
import org.lightning.serverMonitor.Main;
import org.lightning.serverMonitor.javalinWebapp.JavalinWebApp;
import org.lightning.serverMonitor.javalinWebapp.OnMessageHandlers;
import org.lightning.serverMonitor.CustomCommand;
import org.lightning.serverMonitor.platform.Platform;

import static org.lightning.serverMonitor.javalinWebapp.JavalinWebApp.DELIMITER;

public class CustomCommandHandler extends OnMessageHandlers {
    public CustomCommandHandler(JavalinWebApp javalinWebApp) {
        super("custom-command", javalinWebApp);
    }

    //We can only have one command running at a time
    private final static Object lock = new Object();

    @Override
    public void onMessage(WsMessageContext ctx, String message) {
        synchronized (lock) {
            String command = message.split(DELIMITER)[1];
            for (CustomCommand customCommand : Main.settings.CUSTOM_COMMANDS) {
                if (customCommand.identifier.equals(command)) {
                    int exit = Platform.SINGLETON.runAppCustomCommand(customCommand, (line) -> {
                        ctx.send("custom-command-output" + DELIMITER + line);
                    });
                    ctx.send("custom-command-exit" + DELIMITER + exit);
                    break;
                }
            }
        }
    }
}
