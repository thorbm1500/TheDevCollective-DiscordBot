package dev.prodzeus.jarvis.bot;

import dev.prodzeus.jarvis.database.Database;
import dev.prodzeus.jarvis.misc.Levels;
import dev.prodzeus.jarvis.misc.MemberWelcome;
import dev.prodzeus.jarvis.misc.Ready;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public enum Bot {
    INSTANCE;

    public static final Database database;

    public final JDA jda;

    Bot() {
        jda = JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
                .addEventListeners(new Ready())
                .build();
        try { jda.awaitReady(); } catch (InterruptedException ignored) {}
        //commandManager = new CommandManager();
    }

    static {
        database = new Database();
    }

    public void initialize() {
        jda.addEventListener(new MemberWelcome());
        jda.addEventListener(new Levels());
        //jda.addEventListener(new CommandListener());
        //commandManager.setupCommands();
    }

}