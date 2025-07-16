package dev.prodzeus.tdcdb.bot;

import dev.prodzeus.tdcdb.commands.CommandListener;
import dev.prodzeus.tdcdb.commands.CommandManager;
import dev.prodzeus.tdcdb.misc.MemberWelcome;
import dev.prodzeus.tdcdb.misc.Ready;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public enum Bot {
    INSTANCE;

    public static final Logger logger = LoggerFactory.getLogger("tdcdb");
    public static final Marker dLog = MarkerFactory.getMarker("discord-log");
    public static final Marker dLogMod = MarkerFactory.getMarker("discord-log-user");
    public BasicDataSource ds = new BasicDataSource();
    public final JDA jda;
    public final CommandManager commandManager;

    Bot() {
        ds.setUrl(Configuration.getDb().get());
        ds.setUsername(Configuration.getDbu().get());
        ds.setPassword(Configuration.getDbp().get());
        ds.setMinIdle(2);
        ds.setMaxIdle(10);

        jda = JDABuilder.createDefault(Configuration.getToken().get())
                .addEventListeners(new Ready())
                .build();

        commandManager = new CommandManager();
    }

    public void initialize() {
        jda.addEventListener(new MemberWelcome());
        jda.addEventListener(new CommandListener());
        commandManager.setupCommands();
    }
}