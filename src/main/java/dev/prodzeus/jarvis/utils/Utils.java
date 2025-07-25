package dev.prodzeus.jarvis.utils;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.configuration.enums.Configuration;
import dev.prodzeus.jarvis.configuration.enums.LogChannel;
import dev.prodzeus.jarvis.configuration.enums.Roles;
import dev.prodzeus.jarvis.enums.CollectiveMember;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
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
            guild = Jarvis.BOT.jda.getGuildById(Configuration.GUILD.id);
        }
        return guild;
    }

    @Contract(pure = true)
    public static MessageChannel getLogChannel(@NotNull final LogChannel channel) {
        return getGuild().getTextChannelById(channel.id);
    }

    @Nullable
    @Contract(pure = true)
    public static CollectiveMember getCollectiveMember(@NotNull final Object memberId, @NotNull final Object serverId) {
        try {
            if (memberId instanceof String s1 && serverId instanceof String s2) return getCollectiveMember(Long.parseLong(s1), Long.parseLong(s2));
            else return getCollectiveMember(Long.parseLong(String.valueOf(memberId)), Long.parseLong(String.valueOf(serverId)));
        } catch (Exception e) {
            Jarvis.LOGGER.warn("Failed to get Member instance. IDs were found to be invalid! Member: {}, Server: {}", memberId, serverId);
            return null;
        }
    }

    public static CollectiveMember getCollectiveMember(final long memberId, final long serverId) {
        return new CollectiveMember(memberId, serverId);
    }

    public static CollectiveMember getCollectiveMember(@NotNull final net.dv8tion.jda.api.entities.Member member) {
        return new CollectiveMember(member.getIdLong(), member.getGuild().getIdLong());
    }

    public static TextChannel getTextChannel(final long id) {
        return getGuild().getTextChannelById(id);
    }

    public static void sendDiscordMessage(@NotNull final LogChannel channel, @NotNull final String message) {
        Jarvis.BOT.jda.getTextChannelById(channel.id).sendMessage(message).queue();
    }
}
