package dev.prodzeus.tdcdb.utils;

import dev.prodzeus.tdcdb.bot.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Utils {

    private static Guild guild = null;
    private static Role staff = null;

    private static Role getStaff() {
        if (staff == null) {
            staff = getGuild().getRoleById(Bot.settings.staffRole);
        }
        return staff;
    }

    @Contract(pure = true)
    public static boolean isStaff(@NotNull final List<Role> roles) {
        return roles.contains(getStaff());
    }

    public static Guild getGuild() {
        if (guild == null) {
            guild = Bot.INSTANCE.jda.getGuildById(Bot.settings.guild);
        }
        return guild;
    }
}
