package dev.prodzeus.tdcdb.bot;

public class Main {
    public static void main(String[] args) {
        Configuration.initialize(args);
        Bot.INSTANCE.initialize();
    }
}