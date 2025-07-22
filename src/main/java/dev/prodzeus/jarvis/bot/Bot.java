package dev.prodzeus.jarvis.bot;

import dev.prodzeus.jarvis.database.Database;
import dev.prodzeus.jarvis.games.Count;
import dev.prodzeus.jarvis.misc.Levels;
import dev.prodzeus.jarvis.misc.MemberWelcome;
import dev.prodzeus.jarvis.misc.Ready;
import dev.prodzeus.jarvis.misc.Suggestion;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public enum Bot {
    INSTANCE;

    public static final Database database;
    public final JDA jda;

    Bot() {
        this.jda = JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
                .addEventListeners(new Ready())
                .build();
        try { this.jda.awaitReady(); } catch (InterruptedException ignored) {}
    }

    static {
        database = new Database();
    }

    public void initialize() {
        jda.addEventListener(new MemberWelcome());
        jda.addEventListener(new Levels());
        jda.addEventListener(new Count());
        jda.addEventListener(new Suggestion());
    }

}