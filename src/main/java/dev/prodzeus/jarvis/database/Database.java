package dev.prodzeus.jarvis.database;

import dev.prodzeus.jarvis.logger.Logger;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

@SuppressWarnings("unused")
public class Database {

    private Connection connection;

    private final String host;
    private final int port;
    private final String databaseName;
    private final String user;
    private final String password;

    public Database() {
        Map<String,String> env = System.getenv();
        this.host = env.getOrDefault("DB_HOST","None.");
        this.port = Integer.parseInt(env.getOrDefault("DB_PORT","00000"));
        this.databaseName = env.getOrDefault("DB_NAME","members");
        this.user = env.getOrDefault("DB_USER","None.");
        this.password = env.getOrDefault("DB_PASSWORD","None.");

        connect();
        createTables();
    }

    private void connect() {
        try { connection = DriverManager.getConnection("jdbc:mysql://%s:%d/%s".formatted(this.host, this.port, this.databaseName), this.user, this.password); }
        catch (Exception e) { Logger.severe("Connection to the database failed. %s",e.getMessage()); }
    }

    private void reconnect() {
        try { if (connection == null || connection.isClosed()) connect(); }
        catch (Exception e) { Logger.severe("Reconnection to the database failed. %s",e.getMessage()); }
    }

    private void close() {
        try {
            connection.close();
            if (!connection.isClosed()) {
                Logger.severe("Failed to close connection to the database.");
            } else Logger.debug("Closed connection to the database.");
        }
        catch (Exception e) { Logger.severe("Failed to close the connection to the database. %s",e.getMessage()); }
    }

    private void createTables() {
        try {
            connection.createStatement().execute(
                    """
                    CREATE TABLE IF NOT EXISTS members (
                    id               BIGINT PRIMARY KEY,
                    experience       INT DEFAULT 0,
                    correct_counts   INT DEFAULT 0,
                    incorrect_counts INT DEFAULT 0
                    )
                    """);
        }
        catch (Exception e) { Logger.severe("Failed to create database tables. %s",e.getMessage()); }
        finally { close(); }
    }

    public void addMember(@NotNull final String memberId) {
        final Integer id = getMemberId(memberId);
        if (id != null) addMember(id);
        else Logger.warn("Failed to add member to the database. %s is not a valid integer!",memberId);
    }

    public void addMember(final int memberId) {
        reconnect();
        if (memberExists(memberId)) {
            Logger.info("Member with id %s already exists in the database and will not be added.",String.valueOf(memberId));
            return;
        }
        try (var statement = connection.prepareStatement("INSERT INTO members (id) VALUES (?)")) {
            statement.setLong(1, memberId);
            statement.executeUpdate();
            Logger.debug("Added member %s to the database.",String.valueOf(memberId));
        }
        catch (Exception e) { Logger.severe("Failed to create tables. %s",e.getMessage()); }
        finally { close(); }
    }

    public boolean memberExists(@NotNull final String memberId) {
        try {
            return memberExists(Integer.parseInt(memberId));
        } catch (NumberFormatException e) {
            Logger.warn("Unable to check for member in database. %s is not a valid integer!",memberId);
            return false;
        }
    }

    public boolean memberExists(final int memberId) {
        reconnect();
        try (var statement = connection.prepareStatement("SELECT '*' FROM members WHERE id=?")) {
            statement.setInt(1, memberId);
            ResultSet result = statement.executeQuery();
            return result.next();
        } catch (SQLException e) {
            Logger.warn("Failed to check for member %s in the database! %s",String.valueOf(memberId),e.getMessage());
            return false;
        }
    }

    public int getExperience(@NotNull final String memberId) {
        final Integer id = getMemberId(memberId);
        if (id != null) return getExperience(id);
        else Logger.warn("Failed to get experience for member! %s is not a valid integer!",memberId);
        return 0;
    }

    public int getExperience(final int memberId) {
        reconnect();
        try (var statement = connection.prepareStatement("SELECT experience FROM members WHERE id=?")) {
            statement.setInt(1, memberId);
            ResultSet result = statement.executeQuery();
            if (result.next()) return result.getInt("experience");
            else Logger.info("No experience found for member %s in the database!",String.valueOf(memberId));
        } catch (SQLException e) { Logger.warn("Failed to get experience for member %s in the database! %s",String.valueOf(memberId),e.getMessage()); }
        return 0;
    }

    public void updateExperience(@NotNull final String memberId, final int experience) {
        final Integer id = getMemberId(memberId);
        if (id != null) updateExperience(id, experience);
        else Logger.warn("Failed to update experience with %sxp for member! %s is not a valid integer!", String.valueOf(experience), memberId);
    }

    public void updateExperience(final int memberId, final int experience) {
        reconnect();
        try (var statement = connection.prepareStatement("UPDATE members SET experience=? WHERE id=?")) {
            statement.setInt(1, experience);
            statement.setInt(2, memberId);
            statement.executeUpdate();
            Logger.debug("Updated experience for member %s to %s",String.valueOf(memberId),String.valueOf(experience));
        } catch (SQLException e) {
            Logger.warn("Failed to update experience with %sxp for member %s! %s", String.valueOf(experience), String.valueOf(memberId), e.getMessage());
        }
    }

    public void incrementCorrectCount(@NotNull final String memberId) {
        final Integer id = getMemberId(memberId);
        if (id != null) incrementCorrectCount(id);
        else Logger.warn("Unable to increment correct count for member in the database. %s is not a valid integer!",memberId);
    }

    public void incrementCorrectCount(final int memberId) {
        reconnect();
        try (var statement = connection.prepareStatement("UPDATE members SET correct_counts=correct_counts+1 WHERE id=?")) {
            statement.setInt(1, memberId);
            statement.executeUpdate();
        } catch (SQLException e) {
            Logger.warn("Failed to increment correct count for member in the database. %s",e.getMessage());
        }
    }

    public void incrementIncorrectCount(@NotNull final String memberId) {
        final Integer id = getMemberId(memberId);
        if (id != null) incrementIncorrectCount(id);
        else Logger.warn("Unable to increment incorrect count for member in the database. %s is not a valid integer!",memberId);
    }

    public void incrementIncorrectCount(final int memberId) {
        reconnect();
        try (var statement = connection.prepareStatement("UPDATE members SET incorrect_counts=incorrect_counts+1 WHERE id=?")) {
            statement.setInt(1, memberId);
            statement.executeUpdate();
        } catch (SQLException e) {
            Logger.warn("Failed to increment incorrect count for member in the database. %s",e.getMessage());
        }
    }

    public Pair<Integer,Integer> getUserCounts(final String memberId) {
        final Integer id = getMemberId(memberId);
        if (id != null) return getUserCounts(id);
        else Logger.warn("Failed to get counts for member in the database. %s is not a valid integer!", memberId);
        return Pair.of(0,0);
    }

    public Pair<Integer,Integer> getUserCounts(final int memberId) {
        reconnect();
        try (var statement = connection.prepareStatement("SELECT correct_counts,incorrect_counts FROM members WHERE id=?")) {
            statement.setInt(1, memberId);
            ResultSet result = statement.executeQuery();
            if (result.next()) return Pair.of(result.getInt("correct_counts"), result.getInt("incorrect_counts"));
        }
        catch (SQLException e) { Logger.warn("Failed to get counts for member %s in the database. ",String.valueOf(memberId),e.getMessage()); }
        return Pair.of(0,0);
    }

    @Nullable
    private Integer getMemberId(@NotNull final Object o) {
        try { return ((Number) o).intValue(); }
        catch (Exception ignored) { return null; }
    }
}