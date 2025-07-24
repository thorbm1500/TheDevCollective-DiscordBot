package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.enums.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MessageListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        final long userId = e.getAuthor().getIdLong();
        if (userId == Bot.INSTANCE.jda.getSelfUser().getIdLong() || userId == 1378753035664363670L /* Cody's ID */ || e.isWebhookMessage()) return;
        final String content = e.getMessage().getContentRaw();
        try {
            if (content.toLowerCase().contains("jarvis")) {
                final net.dv8tion.jda.api.entities.emoji.Emoji emoji = Emoji.HAND_WAVE.getEmoji();
                if (emoji != null) e.getMessage().addReaction(emoji).queue();
                else {
                    Bot.INSTANCE.logger.warn("Attempted to get Emoji but got null instead. Emoji: {} ({})",Emoji.HAND_WAVE.formatted, Emoji.HAND_WAVE.id);
                }
            }
        } catch (Exception ex) {
            Bot.INSTANCE.logger.warn("Failed to add reaction to message! {}",ex);
        }
    }

}
