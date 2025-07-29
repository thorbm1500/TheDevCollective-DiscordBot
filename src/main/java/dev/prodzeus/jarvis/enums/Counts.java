package dev.prodzeus.jarvis.enums;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.games.count.CountLevel;

public record Counts(int correctCounts, int incorrectCounts) {
    public int level() {
        return CountLevel.getCountLevel(correctCounts).level;
    }

    public int levelRequirement() {
        return CountLevel.getCountLevel(correctCounts).requirement;
    }

    public String levelIcon() {
        return Jarvis.BOT.getEmojiFormatted(CountLevel.getCountLevel(correctCounts).emoji);
    }

    public static String levelIcon(final int level) {
        return Jarvis.BOT.getEmojiFormatted(CountLevel.of(level).emoji);
    }
}
