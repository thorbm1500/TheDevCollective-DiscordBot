package dev.prodzeus.tdcdb;

import dev.prodzeus.tdcdb.bot.Bot;
import dev.prodzeus.tdcdb.bot.Configuration;

public class Main {
    public static void main(String[] args) {
        Configuration.initialize(args);
        Bot.INSTANCE.initialize();
    }
}