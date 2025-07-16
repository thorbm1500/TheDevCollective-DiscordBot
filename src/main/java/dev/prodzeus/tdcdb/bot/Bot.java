package dev.prodzeus.tdcdb.bot;

import com.google.gson.Gson;
import dev.prodzeus.tdcdb.commands.CommandListener;
import dev.prodzeus.tdcdb.commands.CommandManager;
import dev.prodzeus.tdcdb.misc.Levels;
import dev.prodzeus.tdcdb.misc.MemberWelcome;
import dev.prodzeus.tdcdb.misc.Ready;
import dev.prodzeus.tdcdb.utils.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public enum Bot {
    INSTANCE;

    public static final Logger logger = LoggerFactory.getLogger("tdcdb");
    public static final Marker dLog;
    public static final Marker dLogMod;
    public static Settings settings;

    public Executor executor = Executors.newVirtualThreadPerTaskExecutor();
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

    static {
        var cfg = System.getenv("TDCDB_CONFIG");
        String config;
        try {
            if (cfg == null) {
                config = new String(Settings.class.getResourceAsStream("/config.json").readAllBytes(), StandardCharsets.UTF_8);
            } else {
                config = Files.readString(Paths.get(cfg));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        settings = new Gson().fromJson(config, Settings.class);
        dLog = Utils.createMarker("discord-log", settings.logChannel);
        dLogMod = Utils.createMarker("discord-user-log", settings.userLogChannel);
    }

    public void initialize() {
        jda.addEventListener(new MemberWelcome());
        jda.addEventListener(new CommandListener());
        jda.addEventListener(new Levels());
        commandManager.setupCommands();
    }

}