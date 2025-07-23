package dev.prodzeus.jarvis.logger;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.configuration.enums.LogChannel;
import dev.prodzeus.jarvis.utils.Utils;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

import static dev.prodzeus.jarvis.configuration.enums.LogChannel.*;
import static java.util.logging.Level.*;

public class Logger {

    private static final java.util.logging.Logger logger;
    private static final Level defaultLevel;
    private static final LogChannel defaultLogChannel;

    static {
        logger = java.util.logging.Logger.getLogger("Jarvis");
        defaultLevel = Level.INFO;
        defaultLogChannel = LOG;
        setLogLevel(defaultLevel);
        logger.info("Jarvis Logger Enabled.");
    }

    public static void raw(@NotNull final String log) {
        logger.info(log);
    }

    public static void raw(@NotNull final String log, @NotNull final Object... f) {
        logger.info(format(log, f));
    }

    public static void log(@NotNull final String log) {
        log(null,null,log);
    }

    public static void log(@NotNull final String log, @NotNull final Object... f) {
        log(format(log,f));
    }

    public static void log(@Nullable final LogChannel channel, @NotNull final String log) {
        log(null,channel,log);
    }

    public static void log(@Nullable final LogChannel channel, @NotNull final String log, @NotNull final Object... f) {
        log(channel,format(log,f));
    }

    public static void log(@Nullable final Level level, @NotNull final String log) {
        log(level,null,log);
    }

    public static void log(@Nullable final Level level, @NotNull final String log, @NotNull final Object... f) {
        log(level,format(log,f));
    }

    public static void log(@Nullable Level level, @Nullable LogChannel channel, @NotNull final String log) {
        if (level == null) level = defaultLevel;
        if (channel == null) channel = defaultLogChannel;

        logger.log(level,log);
        discord(channel, level, log);
    }

    public static void log(@NotNull final Level level, @NotNull final LogChannel channel, @NotNull final String log, @NotNull final Object... f) {
        log(level,channel,format(log,f));
    }

    private static void discord(@Nullable LogChannel channel, @Nullable Level level, @NotNull final String log) {
        if (!isOnline()) return;
        if (level == null) level = defaultLevel;
        if (!isLoggable(level)) return;

        if (channel == null) channel = defaultLogChannel;
        try {
            Utils.getLogChannel(channel)
                    .sendMessage("%s```js\n[%s] %s\n```".formatted(level == SEVERE ? "@here \n" : "",level.getName().toUpperCase(), log))
                    .setSuppressedNotifications(level.intValue() < defaultLevel.intValue())
                    .queue(null, f -> Logger.log(WARNING, "Failed to log message to discord! {}",f));
        } catch (Exception ignored) {}
    }

    private static boolean isLoggable(@NotNull final Level level) {
        return logger.isLoggable(level);
    }

    public static void setLogLevel(@NotNull final Level level) {
        logger.setLevel(level);
    }

    private static String format(@NotNull String log, @NotNull Object... f) {
        String string = log;
        for (Object o : f) {
            switch (o) {
                case Exception e -> string = string.replaceFirst("%s",e.getMessage());
                case Throwable t -> string = string.replaceFirst("%s",t.getMessage());
                case String s -> string = string.replaceFirst("%s",s);
                default -> string = string.replaceFirst("%s",String.valueOf(o));
            }
        }
        return string;
    }

    private static boolean isOnline() {
        return Bot.INSTANCE.jda.getStatus() == JDA.Status.SHUTTING_DOWN;
    }
}
