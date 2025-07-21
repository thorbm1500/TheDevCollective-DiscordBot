package dev.prodzeus.tdcdb.bot;

import dev.prodzeus.tdcdb.configuration.Configuration;

public class Main {
    public static void main(String[] args) {
        Configuration.initialize(args);
        Bot.INSTANCE.initialize();
    }
}