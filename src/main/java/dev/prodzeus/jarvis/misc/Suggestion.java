package dev.prodzeus.jarvis.misc;

import dev.prodzeus.jarvis.configuration.enums.Channels;
import dev.prodzeus.jarvis.enums.Emoji;
import dev.prodzeus.jarvis.logger.Logger;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Suggestion extends ListenerAdapter {

    private final long channelId = Channels.SUGGESTIONS.id;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        final MessageChannel channel = e.getChannel();
        if (channel.getIdLong() != channelId || e.getAuthor().isBot() || e.getAuthor().isSystem() || e.isWebhookMessage()) return;
        e.getMessage().addReaction(Emoji.PLUS.getEmoji()).and(e.getMessage().addReaction(Emoji.MINUS.getEmoji())).queue(null,
                f -> Logger.warn("Failed to add vote reactions to message in suggestions for server %s! %s",e.getGuild().getIdLong(),f));
    }
}
