package dev.prodzeus.jarvis.database;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.configuration.Channels;
import dev.prodzeus.jarvis.configuration.Roles;
import dev.prodzeus.jarvis.enums.Counts;
import dev.prodzeus.jarvis.enums.ServerCount;
import dev.prodzeus.jarvis.member.CollectiveMember;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        Jarvis.LOGGER.debug("New Database instance called. Connecting to database...");
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
        } catch (SQLException e) {
            Jarvis.LOGGER.error("Connection to the database failed. {}", e);
            return false;
        }
        return true;
    }

    private boolean reconnect() {
        try {
            if (connection == null || connection.isClosed()) return connect();
        } catch (SQLException e) {
            Jarvis.LOGGER.error("Reconnection to the database failed. {}", e);
            return false;
        }
        return true;
    }

    private void close() {
        try {
            connection.close();
            if (!connection.isClosed()) {
                Jarvis.LOGGER.error("Failed to close connection to the database. Connection was found to still be open!");
            } else Jarvis.LOGGER.debug("Closed connection to the database.");
        } catch (SQLException e) {
            Jarvis.LOGGER.error("Failed to close connection to the database. {}", e);
        }
    }

    private void createTables() {
        try {
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS members (
                    `id`               BIGINT UNSIGNED NOT NULL,
                    `server`           BIGINT UNSIGNED NOT NULL,
                    `level`            INT UNSIGNED NOT NULL DEFAULT 0,
                    `experience`       BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `correct_counts`   INT UNSIGNED NOT NULL DEFAULT 0,
                    `incorrect_counts` INT UNSIGNED NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`,`server`))
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS servers (
                    `id`                      BIGINT UNSIGNED PRIMARY KEY,
                    `sync_message`            BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `current_count`           INT UNSIGNED NOT NULL DEFAULT 1,
                    `count_highscore`         INT UNSIGNED NOT NULL DEFAULT 0,
                    `time_of_count_highscore` INT UNSIGNED NOT NULL DEFAULT 0)
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS channels (
                    `id`    BIGINT UNSIGNED PRIMARY KEY,
                    `log`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level` BIGINT UNSIGNED NOT NULL DEFAULT 0)
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS roles (
                    `id`        BIGINT UNSIGNED PRIMARY KEY,
                    `member`    BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `staff`     BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_1`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_5`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_10`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_15`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_20`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_25`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_30`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_35`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_40`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_45`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_50`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_55`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_60`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_65`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_70`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_75`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_80`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_85`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_90`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_95`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `level_100` BIGINT UNSIGNED NOT NULL DEFAULT 0)
                    """);
            Jarvis.LOGGER.debug("Database Tables validated.");
        } catch (SQLException e) {
            Jarvis.LOGGER.error("Failed to create database tables. {}", e);
        } finally {
            close();
        }
    }

    @SneakyThrows
    public void validateServer(final long id) {
        if (reconnect()) {
            try {
                try (var statement = connection.prepareStatement("INSERT IGNORE INTO servers (`id`) VALUES (?)")) {
                    statement.setLong(1, id);
                    statement.executeUpdate();
                }
                try (var statement = connection.prepareStatement("INSERT IGNORE INTO channels (`id`) VALUES (?)")) {
                    statement.setLong(1, id);
                    statement.executeUpdate();
                }
                try (var statement = connection.prepareStatement("INSERT IGNORE INTO roles (`id`) VALUES (?)")) {
                    statement.setLong(1, id);
                    statement.executeUpdate();
                }
                Jarvis.LOGGER.debug("Server {} validated in the database.",id);
            } catch (Exception e) {
                connection.rollback();
                Jarvis.LOGGER.error("Failed to validate server {} in the database! {}",id, e);
            }
        } else Jarvis.LOGGER.error("Attempted to validate server {} in the database but failed to connect. Aborting database task...",id);
    }

    public void addMember(final long memberId, final long serverId) {
        if (reconnect()) {
            if (memberExists(memberId, serverId)) return;

            try (var statement = connection.prepareStatement("INSERT INTO members (`id`,`server`) VALUES (?,?)")) {
                statement.setLong(1, memberId);
                statement.setLong(2, serverId);
                statement.executeUpdate();
                Jarvis.LOGGER.debug("Added member to database for server {}.", memberId, serverId);
            } catch (Exception e) {
                Jarvis.LOGGER.error("Failed to add member {} to database for server {}. {}", memberId, serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to add member to database but failed to connect. Aborting database task...");
    }

    public boolean memberExists(final long memberId, final long serverId) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("SELECT * FROM members WHERE id=? AND server=?")) {
                statement.setLong(1, memberId);
                statement.setLong(2, serverId);
                ResultSet result = statement.executeQuery();
                return result.next();
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to check for member {} in database for server {}! {}", memberId, serverId, e);
                return true;
            }
        } else Jarvis.LOGGER.error("Attempted to check if member exists in database but failed to connect. Aborting database task...");
        return true;
    }

    public int getLevel(final long memberId, final long serverId) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("SELECT level FROM members WHERE id=? AND server=?")) {
                statement.setLong(1, memberId);
                statement.setLong(2, serverId);
                ResultSet result = statement.executeQuery();
                if (result.next()) return result.getInt("level");
                else Jarvis.LOGGER.debug("No level found for member {} in database for server {}!", memberId, serverId);
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to get level for member {} in database for server {}! {}", memberId, serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to get level for member in database but failed to connect. Aborting database task...");
        return 0;
    }

    public void updateLevel(final long memberId, final long serverId, final int level) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("UPDATE members SET level=? WHERE id=? AND server=?")) {
                statement.setLong(1, level < 0 ? 0 : level);
                statement.setLong(2, memberId);
                statement.setLong(3, serverId);
                statement.executeUpdate();
                Jarvis.LOGGER.debug("Updated level for member {} to {} for server {}", memberId, level, serverId);
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to update level with {}xp for member {} in database for server {}! {}", level, memberId, serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to update level for member in database but failed to connect. Aborting database task...");
    }

    public long getExperience(final long memberId, final long serverId) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("SELECT experience FROM members WHERE id=? AND server=?")) {
                statement.setLong(1, memberId);
                statement.setLong(2, serverId);
                ResultSet result = statement.executeQuery();
                if (result.next()) return result.getLong("experience");
                else Jarvis.LOGGER.debug("No experience found for member {} in database for server {}!", memberId, serverId);
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to get experience for member {} in database for server {}! {}", memberId, serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to get experience for member in database but failed to connect. Aborting database task...");
        return 0;
    }

    public void updateExperience(final long memberId, final long serverId, final long experience) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("UPDATE members SET experience=? WHERE id=? AND server=?")) {
                statement.setLong(1, experience < 0 ? 0 : experience);
                statement.setLong(2, memberId);
                statement.setLong(3, serverId);
                statement.executeUpdate();
                Jarvis.LOGGER.debug("Updated experience for member {} to {} for server {}", memberId, experience, serverId);
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to update experience with {}xp for member {} in database for server {}! {}", experience, memberId, serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to update experience for member in database but failed to connect. Aborting database task...");
    }

    public void incrementCorrectCount(final long memberId, final long serverId) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("UPDATE members SET correct_counts=correct_counts+1 WHERE id=? AND server=?")) {
                statement.setLong(1, memberId);
                statement.setLong(2, serverId);
                statement.executeUpdate();
                Jarvis.LOGGER.debug("Correct Count incremented for member {} in database for server {}", memberId, serverId);
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to increment correct count for member {} in database for server {}. {}", memberId, serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to increment correct counts for member in database but failed to connect. Aborting database task...");
    }

    public void incrementIncorrectCount(final long memberId, final long serverId) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("UPDATE members SET incorrect_counts=incorrect_counts+1 WHERE id=? AND server=?")) {
                statement.setLong(1, memberId);
                statement.setLong(2, serverId);
                statement.executeUpdate();
                Jarvis.LOGGER.debug("Incorrect Count incremented for member {} in database for server {}", memberId, serverId);
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to increment incorrect count for member {} in database for server {}. {}", memberId, serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to increment incorrect counts for member in database but failed to connect. Aborting database task...");
    }

    @NotNull
    public Counts getUserCounts(@NotNull final CollectiveMember member) {
        return getUserCounts(member.id, member.server);
    }

    @NotNull
    public Counts getUserCounts(final long memberId, final long serverId) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("SELECT correct_counts,incorrect_counts FROM members WHERE id=? AND server=?")) {
                statement.setLong(1, memberId);
                statement.setLong(2, serverId);
                ResultSet result = statement.executeQuery();
                if (result.next()) return new Counts(result.getInt("correct_counts"), result.getInt("incorrect_counts"));
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to get counts for member {} in the database for server {}. {}", memberId, serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to get counts for member in database but failed to connect. Aborting database task...");
        return new Counts(0, 0);
    }

    public void saveUserCounts(@NotNull final CollectiveMember member, final int correctCounts, final int incorrectCounts) {
        saveUserCounts(member.id, member.server, correctCounts, incorrectCounts);
    }

    public void saveUserCounts(final long memberId, final long serverId, final int correctCounts, final int incorrectCounts) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("UPDATE members SET correct_counts=correct_counts+?,incorrect_counts=incorrect_counts+? WHERE id=? AND server=?")) {
                statement.setLong(1, correctCounts);
                statement.setLong(2, incorrectCounts);
                statement.setLong(3, memberId);
                statement.setLong(4, serverId);
                statement.executeUpdate();
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to save counts for member {} in the database for server {}. {}", memberId, serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to save counts for member in database but failed to connect. Aborting database task...");
    }

    public int getServerMemberCount(final long serverId) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("SELECT count(*) as member_count FROM members")) {
                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) return resultSet.getInt("member_count");
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to get member count for server {}! {}", serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to get member count from database for server {} but failed to connect. Aborting database task...", serverId);
        return 0;
    }

    public ServerCount getServerCountStats(final long serverId) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("SELECT current_count,count_highscore,time_of_count_highscore FROM servers WHERE id=?")) {
                statement.setLong(1, serverId);
                final ResultSet result = statement.executeQuery();
                if (result.next()) return new ServerCount(serverId, result.getInt("current_count"), result.getInt("count_highscore"), result.getLong("time_of_count_highscore"));
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to get count stats for server {}! {}", serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to get count stats from database for server {} but failed to connect. Aborting database task...", serverId);
        return new ServerCount(serverId, 0, 0, 0);
    }

    public void saveServerCountStats(@NotNull final ServerCount serverCount) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("UPDATE servers SET current_count=?,count_highscore=?,time_of_count_highscore=? WHERE id=?")) {
                statement.setInt(1, serverCount.current());
                statement.setInt(2, serverCount.highscore());
                statement.setLong(3, serverCount.epochTime());
                statement.setLong(4, serverCount.id());
                statement.executeUpdate();
                Jarvis.LOGGER.debug("Saved counts stats for server {} in database.", serverCount.id());
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to save count stats for server {}! {}", serverCount.id(), e);
            }
        } else Jarvis.LOGGER.error("Attempted to save count stats from database for server {} but failed to connect. Aborting database task...", serverCount.id());
    }

    @NotNull
    public Channels.ChannelIds getChannelIds(final long serverId) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("SELECT log,count,level FROM channels WHERE id=?")) {
                statement.setLong(1, serverId);
                final ResultSet result = statement.executeQuery();
                if(result.next()) {
                    return new Channels.ChannelIds(
                            result.getLong("log"),
                            result.getLong("count"),
                            result.getLong("level"));
                } else {
                    Jarvis.LOGGER.error("No channel ids for server {} in database!", serverId);
                    return new Channels.ChannelIds(0L,0L,0L);
                }
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to get channel ids for server {}! {}", serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to get channel ids from database for server {} but failed to connect. Aborting database task...", serverId);
        return new Channels.ChannelIds(0L, 0L, 0L);
    }

    @SneakyThrows
    public void saveChannelIds(final long serverId, @NotNull final Channels.ChannelIds ids) {
        if (reconnect()) {
            try {
                if (ids.log() != null) {
                    try (var statement = connection.prepareStatement("UPDATE channels SET log=? WHERE id=?")) {
                        statement.setLong(1, ids.log());
                        statement.setLong(2, serverId);
                        statement.executeUpdate();
                    }
                }
                if (ids.count() != null) {
                    try (var statement = connection.prepareStatement("UPDATE channels SET count=? WHERE id=?")) {
                        statement.setLong(1, ids.count());
                        statement.setLong(2, serverId);
                        statement.executeUpdate();
                    }
                }
                if (ids.level() != null) {
                    try (var statement = connection.prepareStatement("UPDATE channels SET level=? WHERE id=?")) {
                        statement.setLong(1, ids.level());
                        statement.setLong(2, serverId);
                        statement.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to save channel ids for server {}! {}", serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to save channel ids to database for server {} but failed to connect. Aborting database task...", serverId);
    }

    @NotNull
    public Roles.RoleIds getRoleIds(final long serverId) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("SELECT member,staff,level_1,level_5,level_10,level_15,level_20,level_25,level_30,level_35,level_40,level_45,level_50,level_55,level_60,level_65,level_70,level_75,level_80,level_85,level_90,level_95,level_100 FROM roles WHERE id=?")) {
                statement.setLong(1, serverId);
                final ResultSet result = statement.executeQuery();
                if (!result.next()) {
                    return new Roles.RoleIds(serverId,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L
                    ,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L);
                } else {
                    return new Roles.RoleIds(serverId, result.getLong("member"), result.getLong("staff"),
                            result.getLong("level_1"), result.getLong("level_5"), result.getLong("level_10"),
                            result.getLong("level_15"), result.getLong("level_20"), result.getLong("level_25"),
                            result.getLong("level_30"), result.getLong("level_35"), result.getLong("level_40"),
                            result.getLong("level_45"), result.getLong("level_50"), result.getLong("level_55"),
                            result.getLong("level_60"), result.getLong("level_65"), result.getLong("level_70"),
                            result.getLong("level_75"), result.getLong("level_80"), result.getLong("level_85"),
                            result.getLong("level_90"), result.getLong("level_95"), result.getLong("level_100"));
                }
            } catch (SQLException e) {
                Jarvis.LOGGER.error("Failed to get role ids for server {}! {}", serverId, e);
            }
        } else Jarvis.LOGGER.error("Attempted to get role ids from database for server {} but failed to connect. Aborting database task...", serverId);
        return new Roles.RoleIds(serverId, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    @SneakyThrows
    public void saveRoleIds(@NotNull final Roles.RoleIds ids) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("UPDATE channels SET ?=? WHERE id=?")) {
                connection.setAutoCommit(false);
                statement.setLong(3, ids.id());

                statement.setString(1, "member");
                statement.setLong(2, ids.member());
                statement.addBatch();
                statement.setString(1, "staff");
                statement.setLong(2, ids.staff());
                statement.addBatch();
                statement.setString(1, "level_1");
                statement.setLong(2, ids.level_1());
                statement.addBatch();
                statement.setString(1, "level_5");
                statement.setLong(2, ids.level_5());
                statement.addBatch();
                statement.setString(1, "level_10");
                statement.setLong(2, ids.level_10());
                statement.addBatch();
                statement.setString(1, "level_15");
                statement.setLong(2, ids.level_15());
                statement.addBatch();
                statement.setString(1, "level_20");
                statement.setLong(2, ids.level_20());
                statement.addBatch();
                statement.setString(1, "level_25");
                statement.setLong(2, ids.level_25());
                statement.addBatch();
                statement.setString(1, "level_30");
                statement.setLong(2, ids.level_30());
                statement.addBatch();
                statement.setString(1, "level_35");
                statement.setLong(2, ids.level_35());
                statement.addBatch();
                statement.setString(1, "level_40");
                statement.setLong(2, ids.level_40());
                statement.addBatch();
                statement.setString(1, "level_45");
                statement.setLong(2, ids.level_45());
                statement.addBatch();
                statement.setString(1, "level_50");
                statement.setLong(2, ids.level_50());
                statement.addBatch();
                statement.setString(1, "level_55");
                statement.setLong(2, ids.level_55());
                statement.addBatch();
                statement.setString(1, "level_60");
                statement.setLong(2, ids.level_60());
                statement.addBatch();
                statement.setString(1, "level_65");
                statement.setLong(2, ids.level_65());
                statement.addBatch();
                statement.setString(1, "level_70");
                statement.setLong(2, ids.level_70());
                statement.addBatch();
                statement.setString(1, "level_75");
                statement.setLong(2, ids.level_75());
                statement.addBatch();
                statement.setString(1, "level_80");
                statement.setLong(2, ids.level_80());
                statement.addBatch();
                statement.setString(1, "level_85");
                statement.setLong(2, ids.level_85());
                statement.addBatch();
                statement.setString(1, "level_90");
                statement.setLong(2, ids.level_90());
                statement.addBatch();
                statement.setString(1, "level_95");
                statement.setLong(2, ids.level_95());
                statement.addBatch();
                statement.setString(1, "level_100");
                statement.setLong(2, ids.level_100());
                statement.addBatch();

                statement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                Jarvis.LOGGER.error("Failed to save role ids for server {}! {}", ids.id(), e);
            } finally {
                connection.setAutoCommit(true);
                connection.close();
            }
        } else Jarvis.LOGGER.error("Attempted to save role ids to database for server {} but failed to connect. Aborting database task...", ids.id());
    }

    public long getSyncMessage(final long id) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("SELECT sync_message FROM servers WHERE id=?")) {
                statement.setLong(1, id);
                final ResultSet result = statement.executeQuery();
                if (result.next()) return result.getLong(1);
            } catch (Exception e) {
                Jarvis.LOGGER.error("Failed to get sync message id for server {}! {}", id, e);
            }
        } else {
            Jarvis.LOGGER.error("Attempted to get sync message id for server {} but failed to connect. Aborting database task...", id);
        }
        return 0;
    }

    public void saveSyncMessage(final long serverId, final long messageId) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("UPDATE servers SET sync_message=? WHERE id=?")) {
                statement.setLong(1, messageId);
                statement.setLong(2, serverId);
                statement.executeUpdate();
            } catch (Exception e) {
                Jarvis.LOGGER.error("Failed to save sync message id for server {} in database! {}", serverId, e);
            }
        } else {
            Jarvis.LOGGER.error("Attempted to save sync message id for server {} in database but failed to connect. Aborting database task...", serverId);
        }
    }

    public void clearSyncMessage(final long serverId) {
        if (reconnect()) {
            try (var statement = connection.prepareStatement("UPDATE servers SET sync_message=0 WHERE id=?")) {
                statement.setLong(1, serverId);
                statement.executeUpdate();
            } catch (Exception e) {
                Jarvis.LOGGER.error("Failed to clear sync message id for server {} in database! {}", serverId, e);
            }
        } else {
            Jarvis.LOGGER.error("Attempted to clear sync message id for server {} in database but failed to connect. Aborting database task...", serverId);
        }
    }
}