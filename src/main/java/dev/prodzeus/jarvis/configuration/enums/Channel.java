package dev.prodzeus.jarvis.configuration.enums;

import dev.prodzeus.jarvis.utils.Utils;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public enum Channel {
    WELCOME(1379132249856807052L),
    COUNT(1379134564340863086L),
    LEVEL(1379134479402143834L),
    COMMANDS(1379134509978488873L),
    AI(1381246280600387686L),
    SUGGESTIONS(1386700208150151198L)
    ;

    public final long id;

    Channel(final long id) {
        this.id = id;
    }

    public TextChannel getChannel() {
        return Utils.getTextChannel(id);
    }
}
