package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.games.count.Count;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Shutdown extends ListenerAdapter {

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        Bot.INSTANCE.logger.clearConsumers();
        Bot.INSTANCE.logger.info("JDA Shutting down.");
        Count.shutdown();
        Bot.INSTANCE.logger.info("Goodbye.");
    }
}
