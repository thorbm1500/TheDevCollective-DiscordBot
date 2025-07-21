package dev.prodzeus.tdcdb.logger;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static java.util.logging.Level.*;

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
            GeneralLogger.warning("Failed to send response. No message was provided!\nChannel: %s (%s)".formatted(channel.getId(), channel.getName()));
            return;
        }

        if (user != null) {
            try {
                user.openPrivateChannel().queue(
                        c -> c.sendMessage(message).queue(deleteAfter > -1 ? s -> s.delete().queueAfter(deleteAfter, TimeUnit.SECONDS) : null),
                        f -> Logger.console(SEVERE, "Failed to send private response!\nUser: %s (%s)\n Error: %s".formatted(user.getId(), user.getName(), f.getMessage())));
            } catch (Exception e) {
                Logger.console(WARNING, "Failed to send private response!\nUser: %s (%s)\nContent: %s\nException: %s".formatted(user.getId(), user.getName(), message, e.getMessage()));
            }
        } else if (channel != null) {
            try {
                channel.sendMessage(message)
                        .queue(
                                deleteAfter > 0 ? s -> s.delete().queueAfter(deleteAfter, TimeUnit.SECONDS) : null,
                                f -> Logger.console(SEVERE, "Failed to send response!\nChannel: %s (%s)\nMessage: %s\n Error: %s".formatted(channel.getId(), channel.getName(), message, f.getMessage())));
            } catch (Exception e) {
                Logger.console(WARNING, "Failed to send response!\nChannel: %s (%s)\nContent: %s\nException: %s".formatted(channel.getId(), channel.getName(), message, e.getMessage()));
            }
        } else Logger.console(WARNING, "Failed to send response!\nChannel: ?\nContent: %s\nError: Channel is null.".formatted(message));
    }
}
