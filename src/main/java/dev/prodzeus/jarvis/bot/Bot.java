package dev.prodzeus.jarvis.bot;

import dev.prodzeus.jarvis.configuration.enums.LogChannel;
import dev.prodzeus.jarvis.database.Database;
import dev.prodzeus.jarvis.enums.Emoji;
import dev.prodzeus.jarvis.games.count.Count;
import dev.prodzeus.jarvis.listeners.*;
import dev.prodzeus.jarvis.listeners.Shutdown;
import dev.prodzeus.jarvis.utils.Utils;
import dev.prodzeus.logger.Level;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public enum Bot {
    INSTANCE;

    public final Logger logger;
    public static final Database database;
    public final JDA jda;

    Bot() {
        this.logger = new SLF4JProvider().getLoggerFactory().getLogger("Jarvis").setLevel(Level.INFO);
        this.jda = JDABuilder.createDefault(System.getenv("TOKEN"))
                .addEventListeners(new Ready())
                .setAutoReconnect(true)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_EXPRESSIONS)
                .enableCache(CacheFlag.EMOJI)
                .build();
    }

    static {
        database = new Database();
    }

    public void initialize() {
        logger.info("Attempting to connect to JDA.");
        while (!jda.getStatus().equals(JDA.Status.CONNECTED)) {
            try { jda.awaitReady(); }
            catch (InterruptedException ignored) {}
        }
        logger.info("JDA Connected.");
        logger.registerConsumer(e -> {
            try {
                this.jda.getTextChannelById(LogChannel.LOG.id)
                        .sendMessage("```js\n%s\n```".formatted(e))
                        .setSuppressedNotifications(true)
                        .queue(null, f -> logger.warn("Failed to log message to discord! {}",f));
            } catch (Exception ignored) {}
        });
        jda.addEventListener(new MemberWelcome());
        jda.addEventListener(new MessageListener());
        jda.addEventListener(new Levels());
        jda.addEventListener(new Count());
        jda.addEventListener(new Shutdown());
        Utils.sendDiscordMessage(LogChannel.LOG, "%s **Enabled**\n-# Enabled: <t:%d:R>"
                .formatted(Emoji.DOT_GREEN.formatted,(System.currentTimeMillis() / 1000)));
    }

}