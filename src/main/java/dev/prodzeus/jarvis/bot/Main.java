package dev.prodzeus.jarvis.bot;

public class Main {
    public static void main(String[] args) {
        Bot.INSTANCE.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (Bot.INSTANCE.jda == null) return;
            Bot.INSTANCE.jda.shutdown();
            try { Thread.sleep(5000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }));
    }
}