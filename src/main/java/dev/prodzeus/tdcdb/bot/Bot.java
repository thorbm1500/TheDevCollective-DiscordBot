package dev.prodzeus.tdcdb.bot;

import dev.prodzeus.tdcdb.commands.CommandListener;
import dev.prodzeus.tdcdb.commands.CommandManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public enum Bot {
    INSTANCE;

    public final JDA jda;
    public final CommandManager commandManager;

    Bot() {
        jda = JDABuilder.createDefault(Configuration.getToken().get())
                .addEventListeners(new CommandListener())
                .build();

        commandManager = new CommandManager();
    }

    public void initialize() {
        commandManager.setupCommands();
    }
}