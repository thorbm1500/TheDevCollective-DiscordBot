package dev.prodzeus.tdcdb.logger;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import java.util.logging.Level;

import static java.util.logging.Level.*;

public abstract class Logger {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("Jarvis");

    protected static void debug(final MessageChannel channel, final String log) {
        if (!isLoggable(FINEST)) return;
        logger.finest(log);
        discord(channel, "[DEBUG] %s".formatted(log));
    }

    protected static void info(final MessageChannel channel, final String log) {
        if (!isLoggable(INFO)) return;
        logger.info(log);
        discord(channel, "[INFO] %s".formatted(log));
    }

    protected static void warning(final MessageChannel channel, final String log) {
        if (!isLoggable(WARNING)) return;
        logger.warning(log);
        discord(channel, "[WARNING] %s".formatted(log));
    }

    protected static void severe(final MessageChannel channel, final String log) {
        if (!isLoggable(SEVERE)) return;
        logger.severe(log);
        discord(channel, "[SEVERE] %s".formatted(log));
    }

    private static void discord(final MessageChannel channel, final String log) {
        try {
            channel.sendMessage("```js\n%s\n```".formatted(log)).queue(null, f -> console(SEVERE, "Failed to log message!\nChannel: %s\nMessage: %s\n Error: %s".formatted(channel.getId(), log, f.getMessage())));
        } catch (Exception e) {
            console(WARNING, "Failed to log message!\nChannel: %s (%s)\nContent: %s\nException: %s".formatted(channel.getId(), channel.getName(), log, e.getMessage()));
        }
    }

    private static boolean isLoggable(final Level level) {
        return logger.isLoggable(level);
    }

    public static void console(final String log) {
        console(INFO, log);
    }

    public static void console(final Level level, final String log) {
        logger.log(level, log);
    }

    public static void setLogLevel(final Level level) {
        logger.setLevel(level);
    }
}
