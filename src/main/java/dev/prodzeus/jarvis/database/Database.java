package dev.prodzeus.jarvis.database;

import dev.prodzeus.jarvis.enums.Counts;
import dev.prodzeus.jarvis.enums.Member;
import dev.prodzeus.jarvis.enums.ServerCount;
import dev.prodzeus.jarvis.logger.Logger;
import dev.prodzeus.jarvis.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static java.util.logging.Level.*;

@SuppressWarnings("unused")
public class Database {

    private Connection connection;

    private static final Map<String, String> env = System.getenv();
    private static final String host = env.getOrDefault("DB_HOST", "None.");
    private final String port = env.getOrDefault("DB_PORT", "00000");
    private final String databaseName = env.getOrDefault("DB_NAME", "None.");
    private final String user = env.getOrDefault("DB_USER", "None.");
    private final String password = env.getOrDefault("DB_PASSWORD", "None.");

    public Database() {
        Logger.database(FINE, "New Database instance called. Attempting to connect to database...");
        if (connect()) {
            Logger.database(INFO, "Database Connected.");
            createTables();
        } else {
            Logger.database(SEVERE, "Attempt unsuccessful. Connection to database failed.");
        }
    }

    private boolean connect() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://%s:%s/%s".formatted(host, port, databaseName), user, password);
            return true;
        } catch (Exception e) {
            Logger.database(SEVERE, "Connection to the database failed. %s", e);
        }
        return false;
    }

    private boolean reconnect() {
        try {
            if (connection == null || connection.isClosed()) return connect();
        } catch (Exception e) {
            Logger.database(SEVERE, "Reconnection to the database failed. %s", e);
        }
        return false;
    }

    private void close() {
        try {
            connection.close();
            if (!connection.isClosed()) {
                Logger.database(SEVERE, "Failed to close connection to the database. Connection was found to still be open!");
            } else Logger.database(FINE, "Closed connection to the database.");
        } catch (Exception e) {
            Logger.database(SEVERE, "Failed to close connection to the database. %s", e);
        }
    }

    private void createTables() {
        try {
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS members (
                    `id`               BIGINT UNSIGNED NOT NULL,
                    `server`           BIGINT UNSIGNED NOT NULL,
                    `level`            INT NOT NULL DEFAULT 0,
                    `experience`       BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `correct_counts`   INT NOT NULL DEFAULT 0,
                    `incorrect_counts` INT NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`,`server`))
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS servers (
                    `id`                      BIGINT UNSIGNED PRIMARY KEY,
                    `current_count`           INT NOT NULL DEFAULT 1,
                    `count_highscore`         INT NOT NULL DEFAULT 0,
                    `time_of_count_highscore` INT NOT NULL DEFAULT 0)
                    """);
            Logger.database(FINE, "Created tables in database.");
        } catch (Exception e) {
            Logger.database(SEVERE, "Failed to create database tables. %s", e);
        } finally {
            close();
        }
    }

    public boolean addMember(@NotNull final Object memberId, @NotNull final Object serverId) {
        final Member member = Utils.getMember(memberId, serverId);
        if (member != null) return addMember(member);
        else Logger.database(WARNING, "Failed to add member to the database. IDs were found to be invalid! Member: %s, Server: %s", memberId, serverId);
        return false;
    }

    public boolean addMember(@NotNull final Member member) {
        if (!reconnect()) {
            Logger.database(WARNING, "Attempted to add member to database but failed. Aborting database task...");
            return false;
        } else if (memberExists(member)) {
            Logger.database(FINE, "Member with id %s already exists in the database and will not be added.", member.id());
            return false;
        }
        try (var statement = connection.prepareStatement("INSERT INTO members (`id`,`server`) VALUES (?,?)")) {
            statement.setLong(1, member.id());
            statement.setLong(2, member.server());
            statement.executeUpdate();
            Logger.database(FINE, "Added member to database for server %s.", member.id(), member.server());
            return true;
        } catch (Exception e) {
            Logger.database(SEVERE, "Failed to add member %s to database for server %s. %s", member.id(), member.server(), e);
        } finally {
            close();
        }
        return false;
    }

    public boolean memberExists(@NotNull final Object memberId, @NotNull final Object serverId) {
        final Member member = Utils.getMember(memberId, serverId);
        if (member != null) return memberExists(member);
        else Logger.database(WARNING, "Failed to check for member in database. IDs were found to be invalid! Member: %s, Server: %s", memberId, serverId);
        return true;
    }

    public boolean memberExists(@NotNull final Member member) {
        if (!reconnect()) {
            Logger.database(WARNING, "Attempted to check if member exists in database but failed. Aborting database task...");
            return true;
        }
        try (var statement = connection.prepareStatement("SELECT * FROM members WHERE id=? AND server=?")) {
            statement.setLong(1, member.id());
            statement.setLong(2, member.server());
            ResultSet result = statement.executeQuery();
            return result.next();
        } catch (SQLException e) {
            Logger.database(WARNING, "Failed to check for member %s in database for server %s! %s", member.id(), member.server(), e);
            return true;
        }
    }

    public long getExperience(@NotNull final Object memberId, @NotNull final Object serverId) {
        final Member member = Utils.getMember(memberId, serverId);
        if (member != null) return getExperience(member);
        else Logger.database(WARNING, "Failed to get experience for member! IDs were found to be invalid! Member: %s, Server: %s", memberId, serverId);
        return 0;
    }

    public long getExperience(@NotNull final Member member) {
        if (!reconnect()) {
            Logger.database(WARNING, "Attempted to get experience for member in database but failed. Aborting database task...");
            return 0;
        }
        try (var statement = connection.prepareStatement("SELECT experience FROM members WHERE id=? AND server=?")) {
            statement.setLong(1, member.id());
            statement.setLong(2, member.server());
            ResultSet result = statement.executeQuery();
            if (result.next()) return result.getLong("experience");
            else Logger.database(INFO, "No experience found for member %s in database for server %s!", member.id(), member.server());
        } catch (SQLException e) {
            Logger.database(WARNING, "Failed to get experience for member %s in database for server %s! %s", member.id(), member.server(), e);
        }
        return 0;
    }

    public void updateExperience(@NotNull final Object memberId, @NotNull final Object serverId, final long experience) {
        final Member member = Utils.getMember(memberId, serverId);
        if (member != null) updateExperience(member, experience);
        else Logger.database(WARNING, "Failed to update experience with %sxp for member! IDs were found to be invalid! Member: %s, Server: %s", experience, memberId, serverId);
    }

    public void updateExperience(@NotNull final Member member, final long experience) {
        if (!reconnect()) {
            Logger.database(WARNING, "Attempted to update experience for member in database but failed. Aborting database task...");
            return;
        }
        try (var statement = connection.prepareStatement("UPDATE members SET experience=? WHERE id=? AND server=?")) {
            statement.setLong(1, experience);
            statement.setLong(2, member.id());
            statement.setLong(3, member.server());
            statement.executeUpdate();
            Logger.database(FINE, "Updated experience for member %s to %s for server %s", member.id(), experience, member.server());
        } catch (SQLException e) {
            Logger.database(SEVERE, "Failed to update experience with %sxp for member %s in database for server %s! %s", experience, member.id(), member.server(), e);
        }
    }

    public void incrementCorrectCount(@NotNull final Object memberId, @NotNull final Object serverId) {
        final Member member = Utils.getMember(memberId, serverId);
        if (member != null) incrementCorrectCount(member);
        else Logger.database(WARNING, "Unable to increment correct count for member in the database. IDs were found to be invalid! Member: %s, Server: %s", memberId, serverId);
    }

    public void incrementCorrectCount(@NotNull final Member member) {
        if (!reconnect()) {
            Logger.database(WARNING, "Attempted to increment correct counts for member in database but failed. Aborting database task...");
            return;
        }
        try (var statement = connection.prepareStatement("UPDATE members SET correct_counts=correct_counts+1 WHERE id=? AND server=?")) {
            statement.setLong(1, member.id());
            statement.setLong(2, member.server());
            statement.executeUpdate();
            Logger.database(FINE, "Correct Count incremented for member %s in database for server %s", member.id(), member.server());
        } catch (SQLException e) {
            Logger.database(WARNING, "Failed to increment correct count for member %s in database for server %s. %s", member.id(), member.server(), e);
        }
    }

    public void incrementIncorrectCount(@NotNull final Object memberId, @NotNull final Object serverId) {
        final Member member = Utils.getMember(memberId, serverId);
        if (member != null) incrementIncorrectCount(member);
        else Logger.database(WARNING, "Unable to increment incorrect count for member in the database. IDs were found to be invalid! Member: %s, Server: %s", memberId, serverId);
    }

    public void incrementIncorrectCount(@NotNull final Member member) {
        if (!reconnect()) {
            Logger.database(WARNING, "Attempted to increment incorrect counts for member in database but failed. Aborting database task...");
            return;
        }
        try (var statement = connection.prepareStatement("UPDATE members SET incorrect_counts=incorrect_counts+1 WHERE id=? AND server=?")) {
            statement.setLong(1, member.id());
            statement.setLong(2, member.server());
            statement.executeUpdate();
            Logger.database(FINE, "Incorrect Count incremented for member %s in database for server %s", member.id(), member.server());
        } catch (SQLException e) {
            Logger.database(WARNING, "Failed to increment incorrect count for member %s in database for server %s. %s", member.id(), member.server(), e);
        }
    }

    @NotNull
    public Counts getUserCounts(@NotNull final Object memberId, @NotNull final Object serverId) {
        final Member member = Utils.getMember(memberId, serverId);
        if (member != null) return getUserCounts(member);
        else Logger.database(WARNING,"Failed to get counts for member %s in database for server %s. IDs were found to be invalid!", memberId, serverId);
        return new Counts(0, 0);
    }

    @NotNull
    public Counts getUserCounts(@NotNull final Member member) {
        if (!reconnect()) {
            Logger.database(WARNING, "Attempted to get counts for member in database but failed. Aborting database task...");
            return new Counts(0, 0);
        }
        try (var statement = connection.prepareStatement("SELECT correct_counts,incorrect_counts FROM members WHERE id=? AND server=?")) {
            statement.setLong(1, member.id());
            statement.setLong(2, member.server());
            ResultSet result = statement.executeQuery();
            if (result.next()) return new Counts(result.getInt("correct_counts"), result.getInt("incorrect_counts"));
        } catch (SQLException e) {
            Logger.database(WARNING, "Failed to get counts for member %s in the database for server %s. %s", member.id(), member.server(), e);
        }
        return new Counts(0, 0);
    }

    public int getMemberCount(final long serverId) {
        if (!reconnect()) {
            Logger.database(WARNING, "Attempted to get member count from database for server %s but failed. Aborting database task...", serverId);
            return 0;
        }
        try (var statement = connection.prepareStatement("SELECT count(*) as member_count FROM members")) {
            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) return resultSet.getInt("member_count");
        }
        catch (SQLException e) {
            Logger.database(SEVERE, "Failed to get member count for server %s! %s", serverId, e);
        }
        return 0;
    }

    public ServerCount getServerCountStats(@NotNull final Object serverId) {
        if (serverId instanceof Long l) return getServerCountStats(l);
        else try {
            return getServerCountStats(Long.parseLong(String.valueOf(serverId)));
        } catch (NumberFormatException e) {
            Logger.database(WARNING,"Failed to parse server id to Long! %s", serverId);
            return new ServerCount(0,0,0,0);
        }
    }

    public ServerCount getServerCountStats(@NotNull final Long serverId) {
        if (!reconnect()) {
            Logger.database(WARNING, "Attempted to get count stats from database for server %s but failed. Aborting database task...", serverId);
            return new ServerCount(serverId,0,0,0);
        }
        try (var statement = connection.prepareStatement("SELECT current_count,count_highscore,time_of_count_highscore FROM servers WHERE id=?")) {
            statement.setLong(1,serverId);
            final ResultSet result = statement.executeQuery();
            if (result.next()) return new ServerCount(serverId,result.getInt("current_count"),result.getInt("count_highscore"),result.getLong("time_of_count_highscore"));
        } catch (SQLException e) {
            Logger.database(SEVERE,"Failed to get count stats for server %s! %s", serverId, e);
        }
        return new ServerCount(serverId,0,0,0);
    }

    public void saveServerCountStats(@NotNull final ServerCount serverCount) {
        if (!reconnect()) {
            Logger.database(WARNING, "Attempted to save count stats from database for server %s but failed. Aborting database task...", serverCount.id());
            return;
        }
        try (var statement = connection.prepareStatement("UPDATE servers SET current_count=?,count_highscore=?,time_of_count_highscore=? WHERE id=?")) {
            statement.setInt(1,serverCount.current());
            statement.setInt(2,serverCount.highscore());
            statement.setLong(3,serverCount.epochTime());
            statement.setLong(4,serverCount.id());
            statement.executeUpdate();
            Logger.database(FINE,"Saved counts stats for server %s in database.",serverCount.id());
        } catch (SQLException e) {
            Logger.database(SEVERE,"Failed to save count stats for server %s! %s", serverCount.id(), e);
        }
    }
}