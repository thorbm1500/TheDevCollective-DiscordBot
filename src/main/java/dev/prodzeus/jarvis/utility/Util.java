package dev.prodzeus.jarvis.utility;

import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class Util {

    private static final Logger LOGGER = SLF4JProvider.get().getLoggerFactory().getLogger("Utilities");

    @SneakyThrows
    public static boolean isValidMessageEvent(@NotNull MessageReceivedEvent event) {
        final User user = event.getAuthor();
        return !(user.isBot() || user.isSystem() || event.isWebhookMessage() || event.getMessage().getContentRaw().length() > 2000);
    }

    public static void sendMessage(@NotNull final MessageChannel channel, @NotNull final String message) {
        sendMessage(channel,message,0,TimeUnit.NANOSECONDS);
    }

    public static void sendMessage(@NotNull final MessageChannel channel, @NotNull final String message, @NotNull final String nonce) {
        sendMessage(channel,message, nonce,0,TimeUnit.NANOSECONDS);
    }

    public static void sendMessage(@NotNull final MessageChannel channel, @NotNull final String message, final long delay) {
        sendMessage(channel,message,"",delay,TimeUnit.MILLISECONDS);
    }

    public static void sendMessage(@NotNull final MessageChannel channel, @NotNull final String message, final long delay, @NotNull final TimeUnit unit) {
        sendMessage(channel,message,"",delay,unit);
    }

    public static void sendMessage(@NotNull final MessageChannel channel, @NotNull final String message, @NotNull final String nonce, final long delay, final TimeUnit unit) {
        try {
            channel.sendMessage(message).setNonce(nonce).queueAfter(delay,unit,
                    s -> LOGGER.trace("Message sent: {} \n{}", message.substring(0, Math.min(50, message.length()))),
                            f -> LOGGER.warn("Failed to send message: {} \n{}", message.substring(0, Math.min(50, message.length())), f));
        } catch (Exception e) {
            LOGGER.warn("Failed to send message: {} \n{}", message.substring(0, Math.min(50, message.length())), e);
        }
    }
}
