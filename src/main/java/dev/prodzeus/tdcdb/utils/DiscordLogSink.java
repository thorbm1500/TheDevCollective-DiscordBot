package dev.prodzeus.tdcdb.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import dev.prodzeus.tdcdb.bot.Bot;
import dev.prodzeus.tdcdb.bot.Configuration;
import dev.prodzeus.tdcdb.commands.ai.CommandAI;

public class DiscordLogSink extends AppenderBase<ILoggingEvent> {
    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        iLoggingEvent.getMarkerList().stream().forEach(marker -> {
            String channel;
            if (marker.getName().equals(CommandAI.aiMarker.getName())) {
                channel = Configuration.getAiLogChannel().get();
            } else if (marker.getName().equals(Bot.dLog.getName())) {
                channel = Configuration.getLogChannel().get();
            } else if (marker.getName().equals(Bot.dLogMod.getName())) {
                channel = Configuration.getUserLogChannel().get();
            } else {
                return;
            }
            Utils.getGuild().getTextChannelById(channel).sendMessage(String.format("[%s] %s", iLoggingEvent.getLevel(), iLoggingEvent.getFormattedMessage())).queue();
        });
    }
}
