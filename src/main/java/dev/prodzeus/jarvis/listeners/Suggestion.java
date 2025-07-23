package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.configuration.enums.Channel;
import dev.prodzeus.jarvis.enums.Emoji;
import dev.prodzeus.jarvis.logger.Logger;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class Suggestion extends ListenerAdapter {

    private final long channelId = Channel.SUGGESTIONS.id;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        final MessageChannel channel = e.getChannel();
        if (channel.getIdLong() != channelId || e.getAuthor().isBot() || e.getAuthor().isSystem() || e.isWebhookMessage()) return;
        e.getMessage().addReaction(Emoji.PLUS.getEmoji()).and(e.getMessage().addReaction(Emoji.MINUS.getEmoji())).queue(null,
                f -> Logger.log(Level.WARNING,"Failed to add vote reactions to message in suggestions for server %s! %s",e.getGuild().getIdLong(),f));
    }
}
