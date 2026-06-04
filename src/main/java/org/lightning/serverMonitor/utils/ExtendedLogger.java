package org.lightning.serverMonitor.utils;

import org.lightning.serverMonitor.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtendedLogger {

    private final Logger LOGGER;

    public ExtendedLogger() {
        LOGGER = LoggerFactory.getLogger("ServerMonitor");
    }

    private final void notify(String severity, String message) {
        if (Main.config.DISCORD_WEBHOOK_URL != null && !Main.config.DISCORD_WEBHOOK_URL.isBlank()) {
            DiscordUtils.sendWebhookMessage(Main.config.DISCORD_WEBHOOK_URL,
                    Main.config.SERVER_NAME + " [" + severity + "]: `" + message + "`");
        }
    }

    public void info(String msg) {
        LOGGER.info(msg);
    }

    public void notification(String msg) {
        info(msg);
        notify("INFO", msg);
    }

    public void error(String msg, Throwable e) {
        LOGGER.error(msg, e);
        notify("ERROR", msg);
    }

    public void warn(String msg) {
        LOGGER.warn(msg);
        notify("WARN", msg);
    }

    public void warn(String msg, Throwable e) {
        LOGGER.warn(msg, e);
        notify("WARN", msg);
    }

    public void debug(String msg) {
        //Javalin uses slf4j and it dumps a TON of debug messages
//        if (Main.DEV_ENV) LOGGER.info(msg);
        LOGGER.debug(msg);
    }

}
