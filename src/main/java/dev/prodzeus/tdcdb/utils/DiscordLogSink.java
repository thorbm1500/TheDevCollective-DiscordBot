package dev.prodzeus.tdcdb.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.Marker;

import java.util.HashMap;
import java.util.Map;

public class DiscordLogSink extends AppenderBase<ILoggingEvent> {
    public static Map<Marker, String> markerChannelMap = new HashMap<>();

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        iLoggingEvent.getMarkerList().stream().forEach(marker -> {
            if (!markerChannelMap.containsKey(marker)) return;
            Utils.getGuild().getTextChannelById(markerChannelMap.get(marker)).sendMessage(String.format("[%s] %s", iLoggingEvent.getLevel(), iLoggingEvent.getFormattedMessage())).queue();
        });
    }
}
