package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static dev.prodzeus.jarvis.utility.Util.isValidMessageEvent;

public class MessageListener extends ListenerAdapter {

    private static final Logger LOGGER = SLF4JProvider.get().getLogger("Messages");

    public MessageListener() {}

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (!isValidMessageEvent(e)) return;
        final String content = e.getMessage().getContentRaw();
        try {
            if (content.toLowerCase().contains("jarvis")) {
                final Emoji emoji = Jarvis.getEmoji("hand_wave");
                if (emoji != null) e.getMessage().addReaction(emoji).queue();
                else LOGGER.error("Attempted to get Emoji but got null instead. Emoji: hand_wave");
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to add reaction to message! {}",ex);
        }
    }
}
