package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.logger.Level;
import dev.prodzeus.logger.event.components.EventHandler;
import dev.prodzeus.logger.event.components.EventListener;
import dev.prodzeus.logger.event.events.log.GenericLogEvent;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LogListener implements EventListener {

    private final TextChannel channel;
    private final ConcurrentLinkedQueue<MessageCreateAction> messages = new ConcurrentLinkedQueue<>();

    public LogListener(@NotNull final TextChannel channel) {
        this.channel = channel;
        Executors
                .newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> {
                    try { while (messages.peek() != null) messages.poll().queue(); }
                    catch (Exception ignored) {}
                }, 5, 10, TimeUnit.SECONDS);
    }

    @EventHandler
    public void onLogMessage(@NotNull final GenericLogEvent event) {
        if (event.getLevel().getWeight() < Level.INFO.getWeight() || event.getLevel().getWeight() < Jarvis.LOGGER.getLevel().getWeight()) return;
        final String message = "```js\n" + (event.getFormattedLogNoColor().length() > 1900
                ? event.getFormattedLogNoColor().substring(0, 1900) + "..."
                : event.getFormattedLogNoColor()) + "\n```";

        messages.offer(channel
                .sendMessage(message)
                .setSuppressedNotifications(true)
                .setSuppressEmbeds(true));
    }
}
