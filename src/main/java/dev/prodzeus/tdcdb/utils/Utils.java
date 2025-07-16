package dev.prodzeus.tdcdb.utils;

import dev.prodzeus.tdcdb.bot.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class Utils {
    private static Role staff = null;

    private static Role getStaff() {
        if (staff == null) {
            staff = getGuild().getRoleById(Bot.settings.staffRole);
        }
        return staff;
    }

    public static boolean isStaff(List<Role> roles) {
        return roles.contains(getStaff());
    }

    private static Guild guild = null;

    public static Guild getGuild() {
        if (guild == null) {
            guild = Bot.INSTANCE.jda.getGuildById(Bot.settings.guild);
        }
        return guild;
    }

    public static PreparedStatement prepareStatement(String query) {
        try {
            return getConnection().prepareStatement(query);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() {
        try {
            return Bot.INSTANCE.ds.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Marker createMarker(String name, String channelId) {
        var marker = MarkerFactory.getMarker(name);
        DiscordLogSink.markerChannelMap.put(marker, channelId);
        return marker;
    }
}
