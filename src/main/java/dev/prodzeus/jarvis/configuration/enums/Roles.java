package dev.prodzeus.jarvis.configuration.enums;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.logger.Logger;
import dev.prodzeus.jarvis.utils.Utils;
import net.dv8tion.jda.api.entities.Member;
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
        return Bot.INSTANCE.jda.getRoleById(id);
    }

    public void addRole(final Member member) {
        Utils.getGuild().addRoleToMember(member.getUser(),getRole()).queue(null, f -> Logger.warn("Failed to add %s role to member %s! %s",name,member.getAsMention(),f));
    }
}
