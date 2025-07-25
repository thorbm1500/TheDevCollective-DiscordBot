package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.games.count.Count;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static dev.prodzeus.jarvis.bot.Jarvis.LOGGER;

public class Shutdown extends ListenerAdapter {

    public Shutdown() {
        LOGGER.debug("New Shutdown Listener created.");
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        Jarvis.LOGGER.info("JDA disconnected. Jarvis shutting down...");
        Jarvis.LOGGER.clearConsumers();
        Count.shutdown();
        Jarvis.LOGGER.info("Goodbye.");
    }
}
