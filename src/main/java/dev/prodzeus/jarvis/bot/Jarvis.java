package dev.prodzeus.jarvis.bot;

import dev.prodzeus.jarvis.database.Database;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;

public class Jarvis {


    public static final SLF4JProvider SLF4J;
    public static final Logger LOGGER;
    public static final Database DATABASE;
    public static final Bot BOT;

    static {
        SLF4J = new SLF4JProvider();
        LOGGER = SLF4J.getLoggerFactory().getLogger("Jarvis");
        DATABASE = new Database();
        LOGGER.info("Jarvis loading...");
        BOT = new Bot();
    }

    public static void main(String[] args) {
        BOT.load();
    }
}