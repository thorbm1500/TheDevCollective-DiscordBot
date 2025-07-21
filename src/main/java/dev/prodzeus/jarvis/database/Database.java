package dev.prodzeus.jarvis.database;

import dev.prodzeus.jarvis.logger.Logger;

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
        catch (Exception e) { Logger.severe("Connection to MySQL Database failed. %s",e.getMessage()); }
    }

    private void reconnect() {
        try { if (connection == null || connection.isClosed()) connect(); }
        catch (Exception e) { Logger.severe("Reconnection to MySQL Database failed. %s",e.getMessage()); }
    }

    private void close() {
        try {
            connection.close();
            if (!connection.isClosed()) {
                Logger.severe("Failed to close connection to MySQL Database.");
            } else Logger.debug("Closed connection to MySQL Database.");
        }
        catch (Exception e) { Logger.severe("Failed to close the connection to the MySQL Database. %s",e.getMessage()); }
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
        catch (Exception e) { Logger.severe("Failed to create tables. %s",e.getMessage()); }
        finally { close(); }
    }

    public void addMember(final String memberId) {
        try {
            addMember(Integer.parseInt(memberId));
        } catch (NumberFormatException e) {
            Logger.warn("Unable to add member to database. %s is not a valid integer!",memberId);
        }
    }

    public void addMember(final int memberId) {
        if (memberExists(memberId)) {
            Logger.info("Member with id=%s already exists in the database!",String.valueOf(memberId));
            return;
        }
        try (var statement = connection.prepareStatement("INSERT INTO members (id) VALUES (?)")) {
            statement.setLong(1, memberId);
            statement.executeUpdate();
            Logger.debug("Added member %s to database.",String.valueOf(memberId));
        }
        catch (Exception e) { Logger.severe("Failed to create tables. %s",e.getMessage()); }
        finally { close(); }
    }

    public boolean memberExists(final String memberId) {
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
            Logger.warn("Failed to check for member %s in database! %s",String.valueOf(memberId),e.getMessage());
            return false;
        }
    }

    public int getExperience(final String memberId) {
        try {
            return getExperience(Integer.parseInt(memberId));
        } catch (NumberFormatException e) {
            Logger.warn("Unable to get experience for member %s, as the provided id is not a valid integer!",memberId);
            return 0;
        }
    }

    public int getExperience(final long memberId) {
        try {
            return getExperience((int) memberId);
        } catch (Exception e) {
            Logger.warn("Unable to get experience for member %s, as the provided id is not a valid integer!",String.valueOf(memberId));
            return 0;
        }
    }


    public int getExperience(final int memberId) {
        try (var statement = connection.prepareStatement("SELECT experience FROM members WHERE id=?")) {
            statement.setInt(1, memberId);
            ResultSet result = statement.executeQuery();
            if (result.next()) return result.getInt(1);
            else {
                Logger.info("No experience found for member %s in the database!",String.valueOf(memberId));
                return 0;
            }
        } catch (SQLException e) {
            Logger.warn("Failed to get experience for member %s in database! %s",String.valueOf(memberId),e.getMessage());
            return 0;
        }
    }

    public void updateExperience(final String memberId, final int experience) {
        try {
            updateExperience(Integer.parseInt(memberId), experience);
        } catch (NumberFormatException e) {
            Logger.warn("Failed to increment experience with %s for member %s! ID provided is not a valid integer!", String.valueOf(experience), memberId);
        }
    }

    public void updateExperience(final long memberId, final int experience) {
        try {
            updateExperience((int) memberId, experience);
        } catch (RuntimeException e) {
            Logger.warn("Failed to increment experience with %s for member %s! ID provided is not a valid integer!", String.valueOf(experience), String.valueOf(memberId));
        }
    }

    public void updateExperience(final int memberId, final int experience) {
        reconnect();
        try (var statement = connection.prepareStatement("UPDATE members SET experience=? WHERE id=?")) {
            statement.setInt(1, experience);
            statement.setInt(2, memberId);
            statement.executeUpdate();
            Logger.debug("Updated experience for member %s to %s",String.valueOf(memberId),String.valueOf(experience));
        } catch (SQLException e) {
            Logger.warn("Failed to increment experience with %s for member %s! %s", String.valueOf(experience), String.valueOf(memberId), e.getMessage());
        }
    }
}