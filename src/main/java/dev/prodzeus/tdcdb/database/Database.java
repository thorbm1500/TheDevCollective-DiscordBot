package dev.prodzeus.tdcdb.database;

import dev.prodzeus.tdcdb.bot.Bot;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Database {

    public static PreparedStatement prepareStatement(@NotNull final String query) {
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

}
