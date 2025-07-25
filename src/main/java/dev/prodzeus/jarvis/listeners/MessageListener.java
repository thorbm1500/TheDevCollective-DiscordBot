package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Jarvis;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static dev.prodzeus.jarvis.bot.Jarvis.LOGGER;

public class MessageListener extends ListenerAdapter {

    public MessageListener() {
        LOGGER.debug("New Message Listener created.");
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        final long userId = e.getAuthor().getIdLong();
        if ( userId == Jarvis.BOT.jda.getSelfUser().getIdLong() || userId == 1378753035664363670L /* Cody's ID */ || e.isWebhookMessage()) return;
        final String content = e.getMessage().getContentRaw();
        try {
            if (content.toLowerCase().contains("jarvis")) {
                final Emoji emoji = Jarvis.BOT.getEmoji("hand_wave");
                if (emoji != null) e.getMessage().addReaction(emoji).queue();
                else Jarvis.LOGGER.warn("Attempted to get Emoji but got null instead. Emoji: hand_wave");
            }
        } catch (Exception ex) {
            Jarvis.LOGGER.warn("Failed to add reaction to message! {}",ex);
        }
    }

}
