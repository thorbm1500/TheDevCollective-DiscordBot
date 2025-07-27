package dev.prodzeus.jarvis.utils;

import dev.prodzeus.jarvis.configuration.enums.Roles;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public class Utils {

    @Contract(pure = true)
    public static boolean isStaff(@NotNull Guild guild, @NotNull final List<Role> roles) {
        return roles.contains(getStaffRole(guild));
    }

    public static boolean isUser(@NotNull final Member member) {
        return isUser(member.getUser());
    }

    public static boolean isUser(@NotNull final User user) {
        return !(user.isBot() || user.isSystem());
    }
}
