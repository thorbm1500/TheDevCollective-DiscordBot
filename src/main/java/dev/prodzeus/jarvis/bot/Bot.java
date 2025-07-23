package dev.prodzeus.jarvis.bot;

import dev.prodzeus.jarvis.database.Database;
import dev.prodzeus.jarvis.enums.Emoji;
import dev.prodzeus.jarvis.games.Count;
import dev.prodzeus.jarvis.listeners.*;
import dev.prodzeus.jarvis.logger.Logger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.internal.utils.JDALogger;

public enum Bot {
    INSTANCE;

    public static final Database database;
    public final JDA jda;

    Bot() {
        JDALogger.setFallbackLoggerEnabled(true);
        this.jda = JDABuilder.createDefault(System.getenv("TOKEN"))
                .addEventListeners(new Ready())
                .setAutoReconnect(true)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();
        Logger.raw("Enabled Gateways: %s",jda.getGatewayIntents());
    }

    static {
        database = new Database();
    }

    public void initialize() {
        while (!jda.getStatus().equals(JDA.Status.CONNECTED)) {
            try { jda.awaitReady(); }
            catch (InterruptedException ignored) {}
        }
        Logger.log("JDA Connected.");
        jda.addEventListener(new MemberWelcome());
        jda.addEventListener(new Levels());
        jda.addEventListener(new Count());
        jda.addEventListener(new Suggestion());
        jda.addEventListener(new Shutdown());
        Logger.log("%s **Bot enabled as %s**\n-# Enabled: <t:%s:R>", Emoji.DOT_GREEN.formatted,jda.getSelfUser().getName(),(System.currentTimeMillis() / 1000));
    }

}