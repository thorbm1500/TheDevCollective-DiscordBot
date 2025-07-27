package dev.prodzeus.jarvis.bot;

import dev.prodzeus.jarvis.commands.CommandHandler;
import dev.prodzeus.jarvis.enums.CachedEmoji;
import dev.prodzeus.jarvis.games.count.CountGameHandler;
import dev.prodzeus.jarvis.listeners.Ready;
import dev.prodzeus.jarvis.listeners.Shutdown;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static dev.prodzeus.jarvis.bot.Jarvis.LOGGER;

public class Bot {

    public final JDA jda;
    public final Map<String, Long> emojis = new HashMap<>();
    public final Set<CachedEmoji> cachedEmojis = new HashSet<>();

    public Bot() {
        LOGGER.info("New Jarvis Bot instance created.");
        this.jda = getJda();
    }

    private JDA getJda() {
        LOGGER.info("Building new JDA instance.");
        final JDA newJdaInstance = JDABuilder.createDefault(System.getenv("TOKEN"))
                .addEventListeners(new Ready())
                .setAutoReconnect(true)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_EXPRESSIONS)
                .enableCache(CacheFlag.EMOJI, CacheFlag.STICKER)
                .build();
        LOGGER.info("Connecting to gateway...");
        while (!newJdaInstance.getStatus().equals(JDA.Status.CONNECTED)) {
            try {
                newJdaInstance.awaitReady();
            } catch (InterruptedException ignored) {
            }
        }
        LOGGER.info("Connected to gateway.");
        return newJdaInstance;
    }

    public void load() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        LOGGER.debug("Loading emojis...");
        jda.retrieveApplicationEmojis().complete().forEach(emoji -> emojis.put(emoji.getName(), emoji.getIdLong()));
        loadEmojis();
        LOGGER.debug("Registering Event Listeners.");
        jda.addEventListener(new CommandHandler());
        jda.addEventListener(new CountGameHandler());
        //jda.addEventListener(new MessageListener());
        //jda.addEventListener(new Levels());
        jda.addEventListener(new Shutdown());
        /*Utils.sendDiscordMessage(LogChannel.LOG, "%s **Enabled**\n-# Since: <t:%d:R>"
                .formatted(getEmojiFormatted("status_green"), (System.currentTimeMillis() / 1000)));
        LOGGER.registerConsumer(s -> {
            try {
                this.jda.getTextChannelById(LogChannel.LOG.id)
                        .sendMessage("```js\n" + s + "\n```")
                        .setSuppressedNotifications(true)
                        .queue(null,
                                f -> {
                                    if (f instanceof CancellationException || f instanceof ContextException) return;
                                    LOGGER.warn("Failed to log message to Discord! {}", f);
                                });
            } catch (Exception ignored) {
            }
        });*/
    }

    public void shutdown() {
        if (jda == null) return;
        if (jda.getStatus() == JDA.Status.SHUTTING_DOWN) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            jda.shutdownNow();
        }
    }

    private HashMap<String, Icon> getLocalEmojis() {
        final HashMap<String, Icon> emojis = new HashMap<>();
        final Set<String> resourcePaths = new HashSet<>();
        String jarPath;
        try {
            jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (Exception e) {
            LOGGER.error("Failed to get path of Jar! {}", e);
            return emojis;
        }
        LOGGER.debug("Loading Jar at path: {}", jarPath);
        try (final JarFile jar = new JarFile(jarPath)) {
            LOGGER.debug("JarFile loaded: {}", jar.getName());
            final Iterator<JarEntry> iterator = jar.entries().asIterator();
            while (iterator.hasNext()) {
                final JarEntry entry = iterator.next();
                if (!entry.isDirectory() && entry.getName().startsWith("emoji/")) resourcePaths.add("/" + entry.getName());
            }
        } catch (Exception e) {
            LOGGER.error("Jar entry reading failed at path /{}! {}", jarPath, e);
        }
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

    private void loadEmojis() {
        final HashMap<String, Icon> localEmojis = getLocalEmojis();
        jda.retrieveApplicationEmojis().complete().forEach(emoji -> cachedEmojis.add(CachedEmoji.cache(emoji)));
        for (final var index : localEmojis.entrySet()) {
            if (cachedEmojis.stream().anyMatch(emoji -> index.getKey().equalsIgnoreCase(emoji.name()))) {
                LOGGER.debug("Emoji {} already exists. Skipping...", index.getKey());
            } else {
                LOGGER.info("Uploading Emoji {} to Jarvis.", index.getKey());
                uploadEmoji(index);
            }
        }
        if (validateEmojis(localEmojis.keySet())) LOGGER.info("All emojis loaded and up-to-date.");
        else LOGGER.warn("Inconsistent emojis found. Please check that all emojis are correct and restart the Bot after.");
    }

    private boolean validateEmojis(final Set<String> localEmojis) {
        LOGGER.debug("Validating emojis...");
        if (cachedEmojis.stream().allMatch(emoji -> localEmojis.contains(emoji.name()))) return true;
        else {
            boolean errors = false;
            final List<String> cachedEmojiNames = new ArrayList<>();
            cachedEmojis.forEach(emoji -> cachedEmojiNames.add(emoji.name()));
            for (final String localEmojiName : localEmojis) {
                if (!cachedEmojiNames.contains(localEmojiName)) {
                    LOGGER.warn("Emoji {} missing from Jarvis.", localEmojiName);
                    errors = true;
                }
            }
            return !errors;
        }
    }

    private void uploadEmoji(@NotNull final Map.Entry<String, Icon> emoji) {
        try {
            CachedEmoji.cache(jda.createApplicationEmoji(emoji.getKey(), emoji.getValue()).submit().get());
        } catch (Exception e) {
            LOGGER.warn("Failed to upload Emoji {} to Jarvis! {}", emoji.getKey(), e);
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
}
