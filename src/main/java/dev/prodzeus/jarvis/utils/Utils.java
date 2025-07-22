package dev.prodzeus.jarvis.utils;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.configuration.enums.Configuration;
import dev.prodzeus.jarvis.configuration.enums.LogChannels;
import dev.prodzeus.jarvis.configuration.enums.Roles;
import dev.prodzeus.jarvis.enums.Member;
import dev.prodzeus.jarvis.logger.Logger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Utils {

    private static Guild guild = null;
    private static Role staff = null;

    @Contract(pure = true)
    private static Role getStaffRole() {
        if (staff == null) {
            staff = getGuild().getRoleById(Roles.STAFF.id);
        }
        return staff;
    }

    @Contract(pure = true)
    public static boolean isStaff(@NotNull final List<Role> roles) {
        return roles.contains(getStaffRole());
    }

    @Contract(pure = true)
    public static Guild getGuild() {
        if (guild == null) {
            guild = Bot.INSTANCE.jda.getGuildById(Configuration.GUILD.id);
        }
        return guild;
    }

    @Contract(pure = true)
    public static MessageChannel getLogChannel(@NotNull final LogChannels channel) {
        return getGuild().getTextChannelById(channel.id);
    }

    @Nullable
    @Contract(pure = true)
    public static Member getMember(@NotNull final Object memberId, @NotNull final Object serverId) {
        try {
            if (memberId instanceof String s1 && serverId instanceof String s2) return getMember(Long.parseLong(s1), Long.parseLong(s2));
            else return getMember(Long.parseLong(String.valueOf(memberId)), Long.parseLong(String.valueOf(serverId)));
        } catch (Exception e) {
            Logger.warn("Failed to get Member instance. IDs were found to be invalid! Member: %s, Server: %s", memberId, serverId);
            return null;
        }
    }

    public static Member getMember(final long memberId, final long serverId) {
        return new Member(memberId, serverId);
    }

    public static Member getMember(@NotNull final net.dv8tion.jda.api.entities.Member member) {
        return new Member(member.getIdLong(), member.getGuild().getIdLong());
    }

    public static TextChannel getTextChannel(final long id) {
        return getGuild().getTextChannelById(id);
    }
}
