package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.logger.Event;
import dev.prodzeus.logger.Listener;
import dev.prodzeus.logger.components.Level;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LogListener extends Listener {

    private final TextChannel channel;
    private final ConcurrentLinkedQueue<MessageCreateAction> messages = new ConcurrentLinkedQueue<>();

    public LogListener(@NotNull final TextChannel channel) {
        super(Jarvis.LOGGER);
        this.channel = channel;
        Executors
                .newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> {
                    try { while (messages.peek() != null) messages.poll().queue(); }
                    catch (Exception ignored) {}
                }, 5, 10, TimeUnit.SECONDS);
    }

    @Override
    public void onGenericLogEvent(@NotNull final Event event) {
        if (event.getLevel().getWeight() < Level.INFO.getWeight() || event.getLevel().getWeight() < Jarvis.LOGGER.getLevel().getWeight()) return;
        final String message = "```js\n" + (event.getRawMessage().length() > 1900
                ? event.getRawMessage().substring(0, 1900) + "..."
                : event.getRawMessage()) + "\n```";

        messages.offer(channel
                .sendMessage(message)
                .setSuppressedNotifications(true)
                .setSuppressEmbeds(true));
    }
}
