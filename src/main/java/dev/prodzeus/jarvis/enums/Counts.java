package dev.prodzeus.jarvis.enums;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.games.count.CountLevel;

public record Counts(int correctCounts, int incorrectCounts) {
    public String level() {
        return Jarvis.BOT.getEmojiFormatted(CountLevel.getCountLevel(correctCounts).emoji);
    }
}
