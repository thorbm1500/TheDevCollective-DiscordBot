package dev.prodzeus.jarvis.response;

import dev.prodzeus.jarvis.bot.Jarvis;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class Response {

    private final User user;
    private final MessageChannel channel;

    private String message = "";
    private int deleteAfter = -1;

    public Response(@NotNull final MessageReceivedEvent event) {
        this(event.getChannel());
    }

    public Response(@NotNull final MessageChannel channel) {
        this.channel = channel;
        this.user = null;
    }

    public Response(@NotNull final User user) {
        channel = null;
        this.user = user;
    }

    public Response message(@NotNull String message, @NotNull final String f) {
        return message(message.formatted(f));
    }

    public Response message(@NotNull String message, @NotNull final String... f) {
        return message(message.formatted(Arrays.stream(f).toList()));
    }

    public Response message(@NotNull final String message) {
        this.message = message;
        return this;
    }

    public Response deleteAfter(final int seconds) {
        this.deleteAfter = seconds;
        return this;
    }

    public void send() {
        if (this.message.isEmpty()) {
            Jarvis.LOGGER.warn("Failed to send response. No message was provided!\nChannel: %s (%s)",channel.getId(), channel.getName());
            return;
        }

        if (user != null) {
            try {
                user.openPrivateChannel().queue(
                        c -> c.sendMessage(message)
                                .queue(deleteAfter > -1 ? s -> s.delete().queueAfter(deleteAfter, TimeUnit.SECONDS) : null),
                        f -> Jarvis.LOGGER.warn("Failed to send private response!\nUser: {} ({})\n Error: {}",user.getId(), user.getName(), f.getMessage()));
            } catch (Exception e) {
                Jarvis.LOGGER.warn("Failed to send private response!\nUser: {} ({})\nContent: {}\nException: {}",user.getId(), user.getName(), message, e.getMessage());
            }
        } else if (channel != null) {
            try {
                channel.sendMessage(message)
                        .queue(
                                deleteAfter > 0 ? s -> s.delete().queueAfter(deleteAfter, TimeUnit.SECONDS) : null,
                                f -> Jarvis.LOGGER.warn("Failed to send response!\nChannel: {} ({})\nMessage: {}\n Error: {}",channel.getId(), channel.getName(), message, f.getMessage()));
            } catch (Exception e) {
                Jarvis.LOGGER.warn("Failed to send response!\nChannel: {} ({})\nContent: {}\nException: {}",channel.getId(), channel.getName(), message, e.getMessage());
            }
        } else Jarvis.LOGGER.warn("Failed to send response!\nChannel: ?\nContent: {}\nError: Channel is null.",message);
    }
}
