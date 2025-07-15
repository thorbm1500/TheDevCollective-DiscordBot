package dev.prodzeus.tdcdb;

import dev.prodzeus.tdcdb.enums.Bot;

public class Main {
    public static void main(String[] args) {
        Configuration.initialize(args);
        Bot.INSTANCE.initialize();
    }
}