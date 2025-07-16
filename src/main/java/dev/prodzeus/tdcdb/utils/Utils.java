package dev.prodzeus.tdcdb.utils;

import dev.prodzeus.tdcdb.bot.Bot;
import dev.prodzeus.tdcdb.bot.Configuration;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class Utils {
    private static Role staff = null;

    private static Role getStaff() {
        if (staff == null) {
            staff = getGuild().getRoleById(Configuration.getStaff().get());
        }
        return staff;
    }

    public static boolean isStaff(List<Role> roles) {
        return roles.contains(getStaff());
    }

    private static Guild guild = null;

    public static Guild getGuild() {
        if (guild == null) {
            guild = Bot.INSTANCE.jda.getGuildById(Configuration.getGuild().get());
        }
        return guild;
    }

    public static PreparedStatement prepareStatement(String query) {
        try {
            return Bot.INSTANCE.ds.getConnection().prepareStatement(query);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
