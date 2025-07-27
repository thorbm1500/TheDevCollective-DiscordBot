package dev.prodzeus.jarvis.configuration.enums;

import dev.prodzeus.jarvis.utils.Utils;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

@SuppressWarnings("unused")
public enum Channel {
    LOG(1379145039242068199L),
    COUNT(1379134564340863086L),
    LEVEL(1379134479402143834L)
    ;

    public final long id;

    Channel(final long id) {
        this.id = id;
    }

    public TextChannel getChannel() {
        return Utils.getTextChannel(id);
    }
}
