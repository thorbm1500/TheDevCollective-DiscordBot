package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Jarvis;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MessageListener extends ListenerAdapter {

    public MessageListener() {}

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        final User user = e.getAuthor();
        if (user.isBot() || user.isSystem() || e.isWebhookMessage()) return;
        final String content = e.getMessage().getContentRaw();
        try {
            if (content.toLowerCase().contains("jarvis")) {
                final Emoji emoji = Jarvis.getEmoji("hand_wave");
                if (emoji != null) e.getMessage().addReaction(emoji).queue();
                else Jarvis.LOGGER.error("Attempted to get Emoji but got null instead. Emoji: hand_wave");
            }
        } catch (Exception ex) {
            Jarvis.LOGGER.error("Failed to add reaction to message! {}",ex);
        }
    }

}
