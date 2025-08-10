package dev.prodzeus.jarvis.bot;

import dev.prodzeus.jarvis.configuration.Channels;
import dev.prodzeus.jarvis.database.Database;
import dev.prodzeus.jarvis.enums.CachedEmoji;
import dev.prodzeus.jarvis.listeners.LogListener;
import dev.prodzeus.jarvis.member.MemberManager;
import dev.prodzeus.logger.Listener;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import dev.prodzeus.logger.components.Level;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Jarvis {

    public static final Logger LOGGER;
    public static final Database DATABASE;
    public static final Bot BOT;
    private static final Set<Runnable> shutdownHooks = new HashSet<>();
    private static final Set<Guild> guilds = new HashSet<>();
    private static Listener discordLogSink;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(Jarvis::shutdown));
        LOGGER = getSLF4J().getLoggerFactory().getLogger("Jarvis");
        LOGGER.setLevel(Level.valueOf(System.getenv("LOG_LEVEL").toUpperCase()))
                .info("Jarvis loading...");
        BOT = new Bot();
        DATABASE = new Database();
        BOT.load();
    }

    public static void main(String[] args) {
        new MemberManager();
    }

    public static void registerShutdownHook(@NotNull final Runnable runnable) {
        LOGGER.trace("Registering shutdown hook {}", runnable.toString());
        shutdownHooks.add(runnable);
    }

    @SneakyThrows
    public static synchronized void shutdown() {
        discordLogSink.destroy();
        LOGGER.info("JDA disconnected. Jarvis shutting down...");
        try {
            shutdownHooks.forEach(Runnable::run);
        } catch (Exception e) {
            LOGGER.error("Failed to execute shutdown hooks! {}",e );
        }
        Jarvis.LOGGER.info("Goodbye!");
        BOT.shutdown();
    }

    public static SLF4JProvider getSLF4J() {
        return SLF4JProvider.get();
    }

    public static JDA jda() {
        return BOT.jda;
    }

    @SneakyThrows
    public static void registerDiscordConsumers(@NotNull final Logger logger) {
        for (final Guild guild : jda().getGuilds()) {
            final TextChannel channel = Channels.getChannel(guild.getIdLong(), Channels.DevChannel.LOG);
            if (channel != null) {
                discordLogSink = new LogListener(channel).register();
                channel.sendMessage("%s **Enabled**\n-# Since: <t:%d:R>"
                                .formatted(getEmojiFormatted("status_green"), (System.currentTimeMillis() / 1000)))
                        .queue(null, f -> logger.error("Failed to send 'Online' message to Log Channel!"));
            }
        }
    }

    @Nullable
    public static Emoji getEmoji(final String name) {
        final CachedEmoji emoji = BOT.getCachedEmoji(name);
        return emoji == null ? null : emoji.emoji();
    }

    @NotNull
    public static String getEmojiFormatted(final String name) {
        final CachedEmoji emoji = BOT.getCachedEmoji(name);
        return emoji == null ? "<emoji:null>" : emoji.formatted();
    }

    public static Collection<Guild> getGuilds() {
        if (guilds.isEmpty()) {
            guilds.addAll(jda().getGuilds());
            guilds.addAll(jda().getGuildCache().asSet());
        }
        return guilds;
    }

    public static Guild getGuild(final long id) {
        return jda().getGuildById(id);
    }

    public static User getUser(final long id) {
        return jda().getUserById(id);
    }

    public static Member getMember(final long serverId, final long memberId) {
        return getGuild(serverId).getMemberById(memberId);
    }
}