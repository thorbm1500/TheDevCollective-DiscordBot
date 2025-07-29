package dev.prodzeus.jarvis.bot;

import dev.prodzeus.jarvis.database.Database;
import dev.prodzeus.jarvis.member.MemberManager;
import dev.prodzeus.logger.Level;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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
        LOGGER.setLevel(Level.valueOf(System.getenv("LOG_LEVEL")));
        DATABASE = new Database();
        LOGGER.info("Jarvis loading...");
        BOT = new Bot();
        Runtime.getRuntime().addShutdownHook(new Thread(BOT::shutdown));
    }

    public static void main(String[] args) {
        BOT.load();
        new MemberManager();
    }

    public static void registerShutdownHook(@NotNull final Runnable runnable) {
        shutdownHooks.add(runnable);
    }

    public static synchronized void shutdown() {
        LOGGER.clearConsumers();
        LOGGER.info("JDA disconnected. Jarvis shutting down...");
        shutdownHooks.forEach(Runnable::run);
        Jarvis.LOGGER.info("Goodbye!");
    }

    public static JDA jda() {
        return BOT.jda;
    }

    @Nullable
    public static Emoji getEmoji(final String name) {
        return BOT.getEmoji(name);
    }

    @NotNull
    public static String getEmojiFormatted(final String name) {
        return BOT.getEmojiFormatted(name);
    }

    public static Collection<Guild> getGuilds() {
        return jda().getGuilds();
    }
}