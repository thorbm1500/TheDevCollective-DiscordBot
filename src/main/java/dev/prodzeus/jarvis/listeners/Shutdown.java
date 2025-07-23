package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.games.Count;
import dev.prodzeus.jarvis.logger.Logger;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Shutdown extends ListenerAdapter {

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        Logger.log("JDA Shutting down.");
        Count.shutdown();
        Logger.log("Goodbye.");
    }
}
