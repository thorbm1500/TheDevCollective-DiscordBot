package dev.prodzeus.jarvis.utility;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public class Util {

    public static boolean isValidMessageEvent(@NotNull MessageReceivedEvent event) {
        final User user = event.getAuthor();
        return !(user.isBot() || user.isSystem() || event.isWebhookMessage() || event.getMessage().getContentRaw().length() > 2000);
    }

}
