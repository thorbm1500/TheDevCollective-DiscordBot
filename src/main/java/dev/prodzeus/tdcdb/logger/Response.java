package dev.prodzeus.tdcdb.logger;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import static java.util.logging.Level.*;

@SuppressWarnings("unused")
public class Response {

    private final User user;
    private final MessageChannel channel;

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

    public void send(@NotNull final MessageChannel ch, @NotNull final String message) {
        try {
            ch.sendMessage(message).queue(null, f -> Logger.console(SEVERE, "Failed to send response!\nChannel: %s (%s)\nMessage: %s\n Error: %s".formatted(ch.getId(), ch.getName(), message, f.getMessage())));
        } catch (Exception e) {
            Logger.console(WARNING, "Failed to send response!\nChannel: %s (%s)\nContent: %s\nException: %s".formatted(channel.getId(), channel.getName(), message, e.getMessage()));
        }
    }

    public void send(@NotNull final String message) {
        if (user == null) {
            if (this.channel != null) send(channel, message);
            else Logger.console(WARNING, "Failed to send response!\nChannel: ?\nContent: %s\nError: Channel is null.".formatted(message));
        }
        else try {
            user.openPrivateChannel().queue(ch -> send(ch, message), f -> Logger.console(SEVERE, "Failed to send private response!\nUser: %s (%s)\n Error: %s".formatted(user.getId(), user.getName(), f.getMessage())));
        } catch (Exception e) {
            Logger.console(WARNING, "Failed to send private response!\nUser: %s (%s)\nContent: %s\nException: %s".formatted(user.getId(), user.getName(), message, e.getMessage()));
        }
    }
}
