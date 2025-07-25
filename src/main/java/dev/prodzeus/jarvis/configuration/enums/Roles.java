package dev.prodzeus.jarvis.configuration.enums;

import dev.prodzeus.jarvis.bot.Jarvis;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.Nullable;

public enum Roles {
    MEMBER("Member",1379135561989750886L),
    STAFF("Staff",1379134761561358488L)
    ;

    private final String name;
    public final long id;

    Roles(final String name, final long id) {
        this.name = name;
        this.id = id;
    }

    @Nullable
    public Role getRole() {
        return Jarvis.BOT.jda.getRoleById(id);
    }
}
