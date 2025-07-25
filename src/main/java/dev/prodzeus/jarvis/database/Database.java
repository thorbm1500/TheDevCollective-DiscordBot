package dev.prodzeus.jarvis.database;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.enums.Counts;
import dev.prodzeus.jarvis.enums.CollectiveMember;
import dev.prodzeus.jarvis.enums.ServerCount;
import dev.prodzeus.jarvis.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.Map;

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
        Jarvis.LOGGER.debug("New Database instance called. Attempting to connect to database...");
        if (connect()) {
            Jarvis.LOGGER.info("Database Connected.");
            createTables();
        } else {
            Jarvis.LOGGER.error("Attempt unsuccessful. Connection to database failed. Shutting down!");
            Jarvis.BOT.jda.shutdown();
        }
    }

    private boolean connect() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://%s:%s/%s".formatted(host, port, databaseName), user, password);
            return true;
        } catch (Exception e) {
            Jarvis.LOGGER.warn("Connection to the database failed. {}", e);
        }
        return false;
    }

    private boolean reconnect() {
        try {
            if (connection == null || connection.isClosed()) return connect();
        } catch (Exception e) {
            Jarvis.LOGGER.warn("Reconnection to the database failed. {}", e);
            return false;
        }
        return true;
    }

    private void close() {
        try {
            connection.close();
            if (!connection.isClosed()) {
                Jarvis.LOGGER.warn("Failed to close connection to the database. Connection was found to still be open!");
            } else Jarvis.LOGGER.debug("Closed connection to the database.");
        } catch (Exception e) {
            Jarvis.LOGGER.warn("Failed to close connection to the database. {}", e);
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
            Jarvis.LOGGER.debug("Created tables in database.");
        } catch (Exception e) {
            Jarvis.LOGGER.warn("Failed to create database tables. {}", e);
        } finally {
            close();
        }
    }

    public boolean addMember(@NotNull final Object memberId, @NotNull final Object serverId) {
        final CollectiveMember collectiveMember = Utils.getCollectiveMember(memberId, serverId);
        if (collectiveMember != null) return addMember(collectiveMember);
        else Jarvis.LOGGER.warn("Failed to add member to the database. IDs were found to be invalid! Member: {}, Server: {}", memberId, serverId);
        return false;
    }

    public boolean addMember(@NotNull final CollectiveMember collectiveMember) {
        if (!reconnect()) {
            Jarvis.LOGGER.warn("Attempted to add member to database but failed to connect. Aborting database task...");
            return false;
        } else if (memberExists(collectiveMember)) {
            Jarvis.LOGGER.debug("Member with id {} already exists in the database and will not be added.", collectiveMember.id());
            return false;
        }
        try (var statement = connection.prepareStatement("INSERT INTO members (`id`,`server`) VALUES (?,?)")) {
            statement.setLong(1, collectiveMember.id());
            statement.setLong(2, collectiveMember.server());
            statement.executeUpdate();
            Jarvis.LOGGER.debug("Added member to database for server {}.", collectiveMember.id(), collectiveMember.server());
            return true;
        } catch (Exception e) {
            Jarvis.LOGGER.warn("Failed to add member {} to database for server {}. {}", collectiveMember.id(), collectiveMember.server(), e);
        } finally {
            close();
        }
        return false;
    }

    public boolean memberExists(@NotNull final Object memberId, @NotNull final Object serverId) {
        final CollectiveMember collectiveMember = Utils.getCollectiveMember(memberId, serverId);
        if (collectiveMember != null) return memberExists(collectiveMember);
        else Jarvis.LOGGER.warn("Failed to check for member in database. IDs were found to be invalid! Member: {}, Server: {}", memberId, serverId);
        return true;
    }

    public boolean memberExists(@NotNull final CollectiveMember collectiveMember) {
        if (!reconnect()) {
            Jarvis.LOGGER.warn("Attempted to check if member exists in database but failed to connect. Aborting database task...");
            return true;
        }
        try (var statement = connection.prepareStatement("SELECT * FROM members WHERE id=? AND server=?")) {
            statement.setLong(1, collectiveMember.id());
            statement.setLong(2, collectiveMember.server());
            ResultSet result = statement.executeQuery();
            return result.next();
        } catch (SQLException e) {
            Jarvis.LOGGER.warn("Failed to check for member {} in database for server {}! {}", collectiveMember.id(), collectiveMember.server(), e);
            return true;
        }
    }

    public long getExperience(@NotNull final Object memberId, @NotNull final Object serverId) {
        final CollectiveMember collectiveMember = Utils.getCollectiveMember(memberId, serverId);
        if (collectiveMember != null) return getExperience(collectiveMember);
        else Jarvis.LOGGER.warn("Failed to get experience for member! IDs were found to be invalid! Member: {}, Server: {}", memberId, serverId);
        return 0;
    }

    public long getExperience(@NotNull final CollectiveMember collectiveMember) {
        if (!reconnect()) {
            Jarvis.LOGGER.warn("Attempted to get experience for member in database but failed to connect. Aborting database task...");
            return 0;
        }
        try (var statement = connection.prepareStatement("SELECT experience FROM members WHERE id=? AND server=?")) {
            statement.setLong(1, collectiveMember.id());
            statement.setLong(2, collectiveMember.server());
            ResultSet result = statement.executeQuery();
            if (result.next()) return result.getLong("experience");
            else Jarvis.LOGGER.debug("No experience found for member {} in database for server {}!", collectiveMember.id(), collectiveMember.server());
        } catch (SQLException e) {
            Jarvis.LOGGER.warn("Failed to get experience for member {} in database for server {}! {}", collectiveMember.id(), collectiveMember.server(), e);
        }
        return 0;
    }

    public void updateExperience(@NotNull final Object memberId, @NotNull final Object serverId, final long experience) {
        final CollectiveMember collectiveMember = Utils.getCollectiveMember(memberId, serverId);
        if (collectiveMember != null) updateExperience(collectiveMember, experience);
        else Jarvis.LOGGER.warn("Failed to update experience with {}xp for member! IDs were found to be invalid! Member: {}, Server: {}", experience, memberId, serverId);
    }

    public void updateExperience(@NotNull final CollectiveMember collectiveMember, final long experience) {
        if (!reconnect()) {
            Jarvis.LOGGER.warn("Attempted to update experience for member in database but failed to connect. Aborting database task...");
            return;
        }
        try (var statement = connection.prepareStatement("UPDATE members SET experience=? WHERE id=? AND server=?")) {
            statement.setLong(1, experience);
            statement.setLong(2, collectiveMember.id());
            statement.setLong(3, collectiveMember.server());
            statement.executeUpdate();
            Jarvis.LOGGER.debug("Updated experience for member {} to {} for server {}", collectiveMember.id(), experience, collectiveMember.server());
        } catch (SQLException e) {
            Jarvis.LOGGER.warn("Failed to update experience with {}xp for member {} in database for server {}! {}", experience, collectiveMember.id(), collectiveMember.server(), e);
        }
    }

    public void incrementCorrectCount(@NotNull final Object memberId, @NotNull final Object serverId) {
        final CollectiveMember collectiveMember = Utils.getCollectiveMember(memberId, serverId);
        if (collectiveMember != null) incrementCorrectCount(collectiveMember);
        else Jarvis.LOGGER.warn("Unable to increment correct count for member in the database. IDs were found to be invalid! Member: {}, Server: {}", memberId, serverId);
    }

    public void incrementCorrectCount(@NotNull final CollectiveMember collectiveMember) {
        if (!reconnect()) {
            Jarvis.LOGGER.warn("Attempted to increment correct counts for member in database but failed to connect. Aborting database task...");
            return;
        }
        try (var statement = connection.prepareStatement("UPDATE members SET correct_counts=correct_counts+1 WHERE id=? AND server=?")) {
            statement.setLong(1, collectiveMember.id());
            statement.setLong(2, collectiveMember.server());
            statement.executeUpdate();
            Jarvis.LOGGER.debug("Correct Count incremented for member {} in database for server {}", collectiveMember.id(), collectiveMember.server());
        } catch (SQLException e) {
            Jarvis.LOGGER.warn("Failed to increment correct count for member {} in database for server {}. {}", collectiveMember.id(), collectiveMember.server(), e);
        }
    }

    public void incrementIncorrectCount(@NotNull final Object memberId, @NotNull final Object serverId) {
        final CollectiveMember collectiveMember = Utils.getCollectiveMember(memberId, serverId);
        if (collectiveMember != null) incrementIncorrectCount(collectiveMember);
        else Jarvis.LOGGER.warn("Unable to increment incorrect count for member in the database. IDs were found to be invalid! Member: {}, Server: {}", memberId, serverId);
    }

    public void incrementIncorrectCount(@NotNull final CollectiveMember collectiveMember) {
        if (!reconnect()) {
            Jarvis.LOGGER.warn("Attempted to increment incorrect counts for member in database but failed to connect. Aborting database task...");
            return;
        }
        try (var statement = connection.prepareStatement("UPDATE members SET incorrect_counts=incorrect_counts+1 WHERE id=? AND server=?")) {
            statement.setLong(1, collectiveMember.id());
            statement.setLong(2, collectiveMember.server());
            statement.executeUpdate();
            Jarvis.LOGGER.debug("Incorrect Count incremented for member {} in database for server {}", collectiveMember.id(), collectiveMember.server());
        } catch (SQLException e) {
            Jarvis.LOGGER.warn("Failed to increment incorrect count for member {} in database for server {}. {}", collectiveMember.id(), collectiveMember.server(), e);
        }
    }

    @NotNull
    public Counts getUserCounts(@NotNull final Object memberId, @NotNull final Object serverId) {
        final CollectiveMember collectiveMember = Utils.getCollectiveMember(memberId, serverId);
        if (collectiveMember != null) return getUserCounts(collectiveMember);
        else Jarvis.LOGGER.warn("Failed to get counts for member {} in database for server {}. IDs were found to be invalid!", memberId, serverId);
        return new Counts(0, 0);
    }

    @NotNull
    public Counts getUserCounts(@NotNull final CollectiveMember collectiveMember) {
        if (!reconnect()) {
            Jarvis.LOGGER.warn("Attempted to get counts for member in database but failed to connect. Aborting database task...");
            return new Counts(0, 0);
        }
        try (var statement = connection.prepareStatement("SELECT correct_counts,incorrect_counts FROM members WHERE id=? AND server=?")) {
            statement.setLong(1, collectiveMember.id());
            statement.setLong(2, collectiveMember.server());
            ResultSet result = statement.executeQuery();
            if (result.next()) return new Counts(result.getInt("correct_counts"), result.getInt("incorrect_counts"));
        } catch (SQLException e) {
            Jarvis.LOGGER.warn("Failed to get counts for member {} in the database for server {}. {}", collectiveMember.id(), collectiveMember.server(), e);
        }
        return new Counts(0, 0);
    }

    public void saveUserCounts(@NotNull final Object memberId, @NotNull final Object serverId, final int correctCounts, final int incorrectCounts) {
        final CollectiveMember collectiveMember = Utils.getCollectiveMember(memberId, serverId);
        if (collectiveMember != null) saveUserCounts(collectiveMember,correctCounts,incorrectCounts);
        else Jarvis.LOGGER.warn("Failed to save counts for member {} in database for server {}. IDs were found to be invalid!", memberId, serverId);
    }

    public void saveUserCounts(@NotNull final CollectiveMember collectiveMember, final int correctCounts, final int incorrectCounts) {
        if (!reconnect()) {
            Jarvis.LOGGER.warn("Attempted to save counts for member in database but failed to connect. Aborting database task...");
            return;
        }
        try (var statement = connection.prepareStatement("UPDATE members SET correct_counts=correct_counts+?,incorrect_counts=incorrect_counts+? WHERE id=? AND server=?")) {
            statement.setLong(1, correctCounts);
            statement.setLong(2, incorrectCounts);
            statement.setLong(3, collectiveMember.id());
            statement.setLong(4, collectiveMember.server());
            statement.executeUpdate();
        } catch (SQLException e) {
            Jarvis.LOGGER.warn("Failed to save counts for member {} in the database for server {}. {}", collectiveMember.id(), collectiveMember.server(), e);
        }
    }

    public int getServerMemberCount(final long serverId) {
        if (!reconnect()) {
            Jarvis.LOGGER.warn("Attempted to get member count from database for server {} but failed to connect. Aborting database task...", serverId);
            return 0;
        }
        try (var statement = connection.prepareStatement("SELECT count(*) as member_count FROM members")) {
            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) return resultSet.getInt("member_count");
        }
        catch (SQLException e) {
            Jarvis.LOGGER.warn("Failed to get member count for server {}! {}", serverId, e);
        }
        return 0;
    }

    public ServerCount getServerCountStats(@NotNull final Object serverId) {
        if (serverId instanceof Long l) return getServerCountStats(l);
        else try {
            return getServerCountStats(Long.parseLong(String.valueOf(serverId)));
        } catch (NumberFormatException e) {
            Jarvis.LOGGER.warn("Failed to parse server id to Long! {}", serverId);
            return new ServerCount(0,0,0,0);
        }
    }

    public ServerCount getServerCountStats(final long serverId) {
        if (!reconnect()) {
            Jarvis.LOGGER.warn("Attempted to get count stats from database for server {} but failed to connect. Aborting database task...", serverId);
            return new ServerCount(serverId,0,0,0);
        }
        try (var statement = connection.prepareStatement("SELECT current_count,count_highscore,time_of_count_highscore FROM servers WHERE id=?")) {
            statement.setLong(1,serverId);
            final ResultSet result = statement.executeQuery();
            if (result.next()) return new ServerCount(serverId,result.getInt("current_count"),result.getInt("count_highscore"),result.getLong("time_of_count_highscore"));
        } catch (SQLException e) {
            Jarvis.LOGGER.warn("Failed to get count stats for server {}! {}", serverId, e);
        }
        return new ServerCount(serverId,0,0,0);
    }

    public void saveServerCountStats(@NotNull final ServerCount serverCount) {
        if (!reconnect()) {
            Jarvis.LOGGER.warn("Attempted to save count stats from database for server {} but failed to connect. Aborting database task...", serverCount.id());
            return;
        }
        try (var statement = connection.prepareStatement("UPDATE servers SET current_count=?,count_highscore=?,time_of_count_highscore=? WHERE id=?")) {
            statement.setInt(1,serverCount.current());
            statement.setInt(2,serverCount.highscore());
            statement.setLong(3,serverCount.epochTime());
            statement.setLong(4,serverCount.id());
            statement.executeUpdate();
            Jarvis.LOGGER.debug("Saved counts stats for server {} in database.",serverCount.id());
        } catch (SQLException e) {
            Jarvis.LOGGER.warn("Failed to save count stats for server {}! {}", serverCount.id(), e);
        }
    }
}