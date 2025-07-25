package dev.prodzeus.jarvis.enums;

import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;

public record CachedEmoji(@NotNull Emoji emoji, @NotNull String name, long id, @NotNull String formatted) {
    public static CachedEmoji cache(@NotNull ApplicationEmoji emoji) {
        return new CachedEmoji(emoji, emoji.getName(), emoji.getIdLong(), emoji.getFormatted());
    }
}