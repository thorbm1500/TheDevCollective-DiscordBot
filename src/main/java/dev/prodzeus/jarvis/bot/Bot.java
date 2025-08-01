package dev.prodzeus.jarvis.bot;

import dev.prodzeus.jarvis.commands.CommandHandler;
import dev.prodzeus.jarvis.configuration.Channels;
import dev.prodzeus.jarvis.enums.CachedEmoji;
import dev.prodzeus.jarvis.games.count.CountGameHandler;
import dev.prodzeus.jarvis.listeners.Levels;
import dev.prodzeus.jarvis.listeners.LogListener;
import dev.prodzeus.jarvis.listeners.MessageListener;
import dev.prodzeus.jarvis.listeners.Ready;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static dev.prodzeus.jarvis.bot.Jarvis.LOGGER;

public class Bot {

    public final JDA jda;
    public final Set<CachedEmoji> cachedEmojis = new HashSet<>();

    public Bot() {
        LOGGER.info("New Jarvis Bot instance created.");
        JDALogger.setFallbackLoggerEnabled(false);
        this.jda = getJda();
        Jarvis.DATABASE.validateServers(this.jda.getGuilds());
    }

    private JDA getJda() {
        LOGGER.info("Building new JDA instance.");
        final JDA newJdaInstance = JDABuilder.createDefault(System.getenv("TOKEN"))
                .addEventListeners(new Ready())
                .setAutoReconnect(true)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_EXPRESSIONS,
                        GatewayIntent.GUILD_PRESENCES)
                .enableCache(CacheFlag.EMOJI,
                        CacheFlag.CLIENT_STATUS,
                        CacheFlag.ACTIVITY)
                .build();
        LOGGER.info("Connecting to gateway...");
        while (!newJdaInstance.getStatus().equals(JDA.Status.CONNECTED)) {
            try {
                newJdaInstance.awaitReady();
            } catch (InterruptedException ignored) {}
        }
        LOGGER.info("Connected to gateway.");
        return newJdaInstance;
    }

    public synchronized void load() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        LOGGER.debug("Loading...");
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(this::loadEmojis);
        executor.submit(this::registerDiscordConsumers);
        executor.submit(this::registerListeners);
        executor.submit(() -> LOGGER.debug("Loading done."));
        executor.shutdown();
    }

    public void shutdown() {
        if (jda != null && jda.getStatus() != JDA.Status.SHUTTING_DOWN) jda.shutdown();
        Jarvis.shutdown();
    }

    @SneakyThrows
    private void loadEmojis() {
        LOGGER.debug("Loading emojis...");
        for (final ApplicationEmoji emoji : jda.retrieveApplicationEmojis().complete(true)) {
            cachedEmojis.add(CachedEmoji.cache(emoji));
        }
        loadAndCompareEmojis();
    }

    private void loadAndCompareEmojis() {
        final HashMap<String, Icon> localEmojis = getLocalEmojis();
        boolean newUploads = false;

        for (final Map.Entry<String, Icon> localEmoji : localEmojis.entrySet()) {
            if (cachedEmojis.stream().anyMatch(cached -> cached.equals(localEmoji.getKey()))) {
                LOGGER.debug("Emoji {} already exists. Skipping...", localEmoji.getKey());
            } else if (localEmoji.getKey().length() <= 32) {
                try {
                    cachedEmojis.add(CachedEmoji.cache(
                            jda.createApplicationEmoji(localEmoji.getKey(), localEmoji.getValue())
                                    .complete(true)));
                    if (!newUploads) newUploads = true;
                    LOGGER.info("Emoji {} successfully uploaded to Jarvis.", localEmoji.getKey());
                } catch (Exception e) {
                    LOGGER.error("Failed to update Emoji {}! {}", localEmoji.getKey(), e);
                }
            } else {
                LOGGER.error("Emoji names must be shorter than 32! Emoji {} has a length of {}!", localEmoji.getKey(), localEmoji.getKey().length());
            }
        }
        if (!newUploads) LOGGER.info("All emojis loaded and up-to-date.");
        else validateEmojis(localEmojis.keySet());
    }

    @SneakyThrows
    private HashMap<String, Icon> getLocalEmojis() {
        final HashMap<String, Icon> emojis = new HashMap<>();
        final Set<String> resourcePaths = new HashSet<>();
        final String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        if (jarPath == null) return emojis;
        LOGGER.debug("Loading Jar at path: {}", jarPath);
        final JarFile jar = new JarFile(jarPath);
        LOGGER.debug("JarFile loaded: {}", jar.getName());
        final Iterator<JarEntry> iterator = jar.entries().asIterator();
        while (iterator.hasNext()) {
            final JarEntry entry = iterator.next();
            if (!entry.isDirectory() && entry.getName().startsWith("emoji/")) resourcePaths.add("/" + entry.getName());
        }
        jar.close();
        for (final String path : resourcePaths) {
            try (InputStream stream = getClass().getResourceAsStream(path)) {
                if (stream != null) {
                    emojis.put(path.substring("/emoji/".length(), path.lastIndexOf(".")), Icon.from(stream));
                    LOGGER.debug("Jar Entry: {}", path);
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to get new InputStream for Jar Entry {}! {}", path, ex);
            }
        }
        return emojis;
    }

    private void validateEmojis(final Set<String> localEmojis) {
        LOGGER.info("Validating emojis...");
        if (cachedEmojis.stream().allMatch(emoji -> localEmojis.contains(emoji.name()))) {
            LOGGER.info("Validation completed! All emojis loaded and up-to-date.");
        } else {
            boolean errors = false;
            final List<String> cachedEmojiNames = new ArrayList<>();
            for (final CachedEmoji emoji : cachedEmojis) {
                cachedEmojiNames.add(emoji.name());
            }
            for (final String localEmojiName : localEmojis) {
                if (!cachedEmojiNames.contains(localEmojiName)) {
                    errors = true;
                    LOGGER.warn("Emoji {} missing from Jarvis.", localEmojiName);
                }
            }
            if (errors) LOGGER.warn("Ensure to delete any invalid emojis from resources and restart the Bot after!");
            else LOGGER.info("Validation completed! All emojis loaded and up-to-date.");
        }
    }

    @Nullable
    public Emoji getEmoji(final String name) {
        final CachedEmoji emoji = getCachedEmoji(name);
        return emoji == null ? null : emoji.emoji();
    }

    @NotNull
    public String getEmojiFormatted(final String name) {
        final CachedEmoji emoji = getCachedEmoji(name);
        return emoji == null ? "`<emoji:null>`" : emoji.formatted();
    }

    @Nullable
    public CachedEmoji getCachedEmoji(final String name) {
        return cachedEmojis.stream().filter(emoji -> emoji.name().equals(name)).findFirst().orElse(null);
    }

    @SneakyThrows
    private void registerDiscordConsumers() {
        for (final Guild guild : jda.getGuilds()) {
            final long logId = Channels.get(guild.getIdLong()).logChannel;
            if (logId != 0L) {
                Jarvis.getSLF4J().registerListener(new LogListener(logId), LOGGER);
                jda.getTextChannelById(logId).sendMessage("%s **Enabled**\n-# Since: <t:%d:R>"
                                .formatted(getEmojiFormatted("status_green"), (System.currentTimeMillis() / 1000)))
                        .queue(null, f -> LOGGER.error("Failed to send 'Online' message to Log Channel!"));
            }
        }
    }

    private void registerListeners() {
        LOGGER.debug("Registering Event Listeners.");
        jda.addEventListener(new CommandHandler());
        jda.addEventListener(new CountGameHandler());
        jda.addEventListener(new MessageListener());
        jda.addEventListener(new Levels());
    }
}
