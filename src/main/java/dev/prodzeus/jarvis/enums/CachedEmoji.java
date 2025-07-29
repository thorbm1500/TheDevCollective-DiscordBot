package dev.prodzeus.jarvis.enums;

import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;

public record CachedEmoji(@NotNull Emoji emoji, @NotNull String name, long id, @NotNull String formatted) {
    public static CachedEmoji cache(@NotNull ApplicationEmoji emoji) {
        return new CachedEmoji(emoji, emoji.getName(), emoji.getIdLong(), emoji.getFormatted());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        else if (this == o) return true;
        else return switch (o) {
                case String s -> s.equals(name);
                case CachedEmoji(Emoji otherEmoji, String otherName, long otherId, String otherFormatted) ->
                        otherEmoji.equals(emoji) && otherName.equals(name) && otherId == id && otherFormatted.equals(formatted);
                default -> false;
            };
    }
}