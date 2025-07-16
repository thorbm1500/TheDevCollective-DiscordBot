package dev.prodzeus.tdcdb.bot;

import dev.prodzeus.tdcdb.commands.CommandListener;
import dev.prodzeus.tdcdb.commands.CommandManager;
import dev.prodzeus.tdcdb.misc.MemberWelcome;
import dev.prodzeus.tdcdb.misc.Ready;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Bot {
    INSTANCE;

    public static final Logger logger = LoggerFactory.getLogger("tdcdb");
    public final JDA jda;
    public final CommandManager commandManager;

    Bot() {
        jda = JDABuilder.createDefault(Configuration.getToken().get())
                .addEventListeners(new Ready())
                .addEventListeners(new MemberWelcome())
                .addEventListeners(new CommandListener())
                .build();

        commandManager = new CommandManager();
    }

    public void initialize() {
        commandManager.setupCommands();
    }
}