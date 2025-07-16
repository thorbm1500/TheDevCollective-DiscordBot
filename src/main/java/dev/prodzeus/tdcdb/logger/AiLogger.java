package dev.prodzeus.tdcdb.logger;

import dev.prodzeus.tdcdb.bot.Bot;
import dev.prodzeus.tdcdb.utils.Utils;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class AiLogger extends Logger {

    public static final MessageChannel channel = Utils.getGuild().getTextChannelById(Bot.settings.aiLogChannel);

    public static void debug(final String log) {
        debug(channel, log);
    }

    public static void info(final String log) {
        info(channel, log);
    }

    public static void warning(final String log) {
        warning(channel, log);
    }

    public static void severe(final String log) {
        severe(channel, log);
    }

}