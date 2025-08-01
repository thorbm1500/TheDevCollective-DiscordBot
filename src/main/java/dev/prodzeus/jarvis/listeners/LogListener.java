package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.logger.event.components.EventHandler;
import dev.prodzeus.logger.event.components.EventListener;
import dev.prodzeus.logger.event.events.log.GenericLogEvent;
import net.dv8tion.jda.api.exceptions.ContextException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;

import static dev.prodzeus.jarvis.bot.Jarvis.LOGGER;

public class LogListener implements EventListener {

    private final long logChannel;

    public LogListener(final long logId) {
        this.logChannel = logId;
    }

    @EventHandler
    public void onLogMessage(@NotNull final GenericLogEvent event) {
        try {
            Jarvis.jda().getTextChannelById(logChannel)
                    .sendMessage("```js\n" + event.getFormattedLogNoColor() + "\n```")
                    .setSuppressedNotifications(true)
                    .queue(null,
                            f -> {
                                if (f instanceof CancellationException || f instanceof ContextException) return;
                                LOGGER.error("Failed to log message to Discord! {}", f);
                            });
        } catch (Exception e) {
            LOGGER.error("Failed to log message to Discord! {}", e);
        }
    }
}
