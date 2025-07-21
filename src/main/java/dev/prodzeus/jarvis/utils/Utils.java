package dev.prodzeus.jarvis.utils;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.configuration.enums.Configuration;
import dev.prodzeus.jarvis.configuration.enums.LogChannels;
import dev.prodzeus.jarvis.configuration.enums.Roles;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Utils {

    private static Guild guild = null;
    private static Role staff = null;

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

    public static Guild getGuild() {
        if (guild == null) {
            guild = Bot.INSTANCE.jda.getGuildById(Configuration.GUILD.id);
        }
        return guild;
    }

    public static MessageChannel getLogChannel(@NotNull final LogChannels channel) {
        return getGuild().getTextChannelById(channel.id);
    }
}
