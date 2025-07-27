package dev.prodzeus.jarvis.bot;

import dev.prodzeus.jarvis.database.Database;
import dev.prodzeus.jarvis.member.MemberManager;
import dev.prodzeus.logger.Level;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class Jarvis {

    private static final SLF4JProvider SLF4J;
    public static final Logger LOGGER;
    public static final Database DATABASE;
    public static final Bot BOT;
    private static final Set<Runnable> shutdownHooks = new HashSet<>();

    static {
        SLF4J = new SLF4JProvider();
        LOGGER = SLF4J.getLoggerFactory().getLogger("Jarvis");
        LOGGER.setLevel(Level.DEBUG);
        DATABASE = new Database();
        LOGGER.info("Jarvis loading...");
        BOT = new Bot();
    }

    public static void main(String[] args) {
        BOT.load();
        new MemberManager();
    }

    public static void registerShutdownHook(@NotNull final Runnable runnable) {
        shutdownHooks.add(runnable);
    }

    public static void shutdown() {
        LOGGER.clearConsumers();
        LOGGER.info("JDA disconnected. Jarvis shutting down...");
        shutdownHooks.forEach(Runnable::run);
        Jarvis.LOGGER.info("Goodbye!");
    }

    public static JDA jda() {
        return BOT.jda;
    }
}