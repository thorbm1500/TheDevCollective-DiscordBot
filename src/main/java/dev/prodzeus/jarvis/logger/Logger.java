package dev.prodzeus.jarvis.logger;

import dev.prodzeus.jarvis.configuration.enums.LogChannels;
import dev.prodzeus.jarvis.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import static java.util.logging.Level.*;

@SuppressWarnings("unused")
public class Logger {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("Jarvis");

    private static void log(@NotNull Level level, @Nullable final LogChannels channel, @NotNull String log) {
        if (!isLoggable(level)) return;
        logger.log(level, log);
        discord(channel == null ? LogChannels.LOG : channel, level, log);
    }

    public static void debug(@NotNull final String log) {
        debug(null, log);
    }

    public static void debug(@NotNull String log, @NotNull final String... f) {
        debug(null, log, f);
    }

    public static void debug(@NotNull String log, @NotNull final Object... f) {
        debug(null, log, f);
    }

    public static void debug(@Nullable final LogChannels channel, @NotNull String log, @NotNull final String... f) {
        debug(channel, format(log, f));
    }

    public static void debug(@Nullable final LogChannels channel, @NotNull String log, @NotNull final Object... f) {
        debug(channel, format(log, f));
    }

    public static void debug(@Nullable final LogChannels channel, @NotNull final String log) {
        log(FINE, channel, log);
    }

    public static void info(@NotNull final String log) {
        info(null, log);
    }

    public static void info(@NotNull String log, @NotNull final String... f) {
        info(null, log, f);
    }

    public static void info(@NotNull String log, @NotNull final Object... f) {
        info(null, log, f);
    }

    public static void info(@Nullable final LogChannels channel, @NotNull String log, @NotNull final String... f) {
        log(INFO, channel, format(log, f));
    }

    public static void info(@Nullable final LogChannels channel, @NotNull String log, @NotNull final Object... f) {
        log(INFO, channel, format(log, f));
    }

    public static void info(@Nullable final LogChannels channel, @NotNull final String log) {
        log(Level.INFO, channel, log);
    }

    public static void warn(@NotNull final String log) {
        warn(null, log);
    }

    public static void warn(@NotNull String log, @NotNull final String... f) {
        warn(null, log, f);
    }

    public static void warn(@NotNull String log, @NotNull final Object... f) {
        warn(null, log, f);
    }

    public static void warn(@Nullable final LogChannels channel, @NotNull String log, @NotNull final String... f) {
        warn(channel, format(log, f));
    }

    public static void warn(@Nullable final LogChannels channel, @NotNull String log, @NotNull final Object... f) {
        warn(channel, format(log, f));
    }

    public static void warn(@Nullable final LogChannels channel, @NotNull final String log) {
        log(Level.WARNING, channel, log);
    }

    public static void severe(@NotNull final String log) {
        severe(null, log);
    }

    public static void severe(@NotNull String log, @NotNull final String... f) {
        severe(null, log, f);
    }

    public static void severe(@NotNull String log, @NotNull final Object... f) {
        severe(null, log, f);
    }

    public static void severe(@Nullable final LogChannels channel, @NotNull String log, @NotNull final String... f) {
        severe(channel, format(log, f));
    }

    public static void severe(@Nullable final LogChannels channel, @NotNull String log, @NotNull final Object... f) {
        severe(channel, format(log, f));
    }

    public static void severe(@Nullable final LogChannels channel, @NotNull final String log) {
        log(Level.SEVERE, channel, log);
    }

    public static void database(@NotNull final String log) {
        database(INFO, log);
    }

    public static void database(@NotNull String log, @NotNull final String... f) {
        database(INFO, format(log, f));
    }

    public static void database(@NotNull String log, @NotNull final Object... f) {
        database(INFO, format(log, f));
    }

    public static void database(@NotNull final Level level, @NotNull String log, @NotNull final String... f) {
        database(level, format(log, f));
    }

    public static void database(@NotNull final Level level, @NotNull String log, @NotNull final Object... f) {
        database(level, format(log, f));
    }

    public static void database(@NotNull final Level level, @NotNull final String log) {
        log(level, LogChannels.DATABASE, log);
    }

    private static void discord(@NotNull final LogChannels channel, @NotNull final Level level, @NotNull final String log) {
        try {
            Utils.getLogChannel(channel)
                    .sendMessage("%s```js\n[%s] %s\n```".formatted(level.intValue() > WARNING.intValue() ? "@here \n" : "",level.getName().toUpperCase(), log))
                    .setSuppressedNotifications(level.intValue() < INFO.intValue())
                    .queue(null, f -> console(SEVERE, "Failed to log message!\nChannel: %s\nMessage: %s\n Error: %s",channel.id,log,f));
        } catch (Exception e) {
            console(WARNING, "Failed to log message!\nChannel: %s\nContent: %s\nException: %s",channel.id,log,e);
        }
    }

    private static boolean isLoggable(@NotNull final Level level) {
        return logger.isLoggable(level);
    }

    public static void console(@NotNull final String log) {
        console(INFO, log);
    }

    public static void console(@NotNull String log, @NotNull final String... f) {
        console(format(log, f));
    }

    public static void console(@NotNull String log, @NotNull final Object... f) {
        console(format(log, f));
    }

    public static void console(@NotNull final Level level, @NotNull final String log) {
        logger.log(level, log);
    }

    public static void console(@NotNull final Level level, @NotNull String log, @NotNull final String... f) {
        console(level, format(log, f));
    }

    public static void console(@NotNull final Level level, @NotNull String log, @NotNull final Object... f) {
        console(level, format(log, f));
    }

    public static void setLogLevel(@NotNull final Level level) {
        logger.setLevel(level);
    }

    private static String format(@NotNull String log, @NotNull String... f) {
        return log.formatted(Arrays.stream(f).toList());
    }

    private static String format(@NotNull String log, @NotNull Object... f) {
        final List<String> placeholders = new ArrayList<>();
        for (Object o : f) {
            switch (o) {
                case Exception e -> placeholders.add(e.getMessage());
                case Throwable t -> placeholders.add(t.getMessage());
                case String s -> placeholders.add(s);
                default -> placeholders.add(String.valueOf(o));
            }
        }
        return log.formatted(placeholders);
    }
}
