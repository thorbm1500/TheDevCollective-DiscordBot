package dev.prodzeus.jarvis.database;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.configuration.Channels;
import dev.prodzeus.jarvis.configuration.Roles;
import dev.prodzeus.jarvis.games.count.CountGameData;
import dev.prodzeus.jarvis.games.count.CountPlayer;
import dev.prodzeus.jarvis.member.CollectiveMember;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Database {

    private Connection connection;
    private static final Logger LOGGER = SLF4JProvider.get().getLogger("Database");

    private static final Map<String, String> ENV = System.getenv();
    private static final String DB_HOST = ENV.getOrDefault("DB_HOST", "None.");
    private static final String DB_PORT = ENV.getOrDefault("DB_PORT", "00000");
    private static final String DB_NAME = ENV.getOrDefault("DB_NAME", "None.");
    private static final String DB_USER = ENV.getOrDefault("DB_USER", "None.");
    private static final String DB_PASSWORD = ENV.getOrDefault("DB_PASSWORD", "None.");

    public Database() {
        if (!connect()) {
            LOGGER.error("Establishing a connection to the database failed. Shutting down!..");
            Jarvis.BOT.jda.shutdown();
            return;
        }

        LOGGER.info("[{}:{}/{}] Database Connected.", DB_HOST, DB_PORT, DB_NAME);
        createTables();
        validateServers();
    }

    private boolean connect() {
        try {
            LOGGER.trace("[DB:{}:{}/{}] Attempting to connect...", DB_HOST, DB_PORT, DB_NAME);
            connection = DriverManager.getConnection("jdbc:mysql://%s:%s/%s".formatted(DB_HOST, DB_PORT, DB_NAME), DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            LOGGER.error("Failed to connect to the database. {}", e);
            return false;
        }
        return true;
    }

    private boolean reconnect(@NotNull final String method) {
        try {
            if (connection == null || connection.isClosed()) {
                if (connect()) {
                    LOGGER.trace("Reconnected to the database.");
                }
            }
            return true;
        } catch (SQLException e) {
            LOGGER.error("Failed to reconnect to the database{}. {}", (method.isEmpty() ? "" : " for Database#" + method), e);
        }
        return false;
    }

    private void close() {
        try {
            connection.close();
            if (!connection.isClosed()) {
                LOGGER.error("Failed to close connection to the database. Connection was found to still be open!");
            } else LOGGER.trace("Closed connection to the database.");
        } catch (SQLException e) {
            LOGGER.error("Failed to close connection to the database. {}", e);
        }
    }

    private void createTables() {
        LOGGER.trace("Validating tables...");
        try {
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS `servers` (
                    `server_id` BIGINT UNSIGNED NOT NULL,
                    PRIMARY KEY (`server_id`))
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS `members` (
                    `member_id`          BIGINT UNSIGNED NOT NULL,
                    `server_id`          BIGINT UNSIGNED NOT NULL,
                    `level`              INT UNSIGNED NOT NULL DEFAULT 0,
                    `experience`         BIGINT NOT NULL DEFAULT 0,
                    `correct_counts`     INT UNSIGNED NOT NULL DEFAULT 0,
                    `incorrect_counts`   INT UNSIGNED NOT NULL DEFAULT 0,
                    `images_sent`        INT UNSIGNED NOT NULL DEFAULT 0,
                    `reactions_given`    INT UNSIGNED NOT NULL DEFAULT 0,
                    `reactions_received` INT UNSIGNED NOT NULL DEFAULT 0,
                    PRIMARY KEY (`member_id`,`server_id`),
                    FOREIGN KEY (`server_id`) REFERENCES servers (`server_id`))
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS `game_data` (
                    `member_id` BIGINT UNSIGNED NOT NULL,
                    `server_id` BIGINT UNSIGNED NOT NULL,
                    `counts`    INT UNSIGNED NOT NULL DEFAULT 0,
                    PRIMARY KEY (`member_id`,`server_id`),
                    FOREIGN KEY (`member_id`,`server_id`) REFERENCES members (`member_id`,`server_id`))
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS `game_data_count` (
                    `server_id`           BIGINT UNSIGNED NOT NULL,
                    `sync_message`        BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `latest_player`       BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    `current_number`      INT UNSIGNED NOT NULL DEFAULT 1,
                    `highscore_announced` TINYINT(1) NOT NULL DEFAULT 0,
                    `highscore`           INT UNSIGNED NOT NULL DEFAULT 1,
                    `highscore_epoch`     BIGINT UNSIGNED NOT NULL DEFAULT (UNIX_TIMESTAMP()),
                    PRIMARY KEY (`server_id`),
                    FOREIGN KEY (`server_id`) REFERENCES servers (`server_id`))
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS `channels` (
                    `server_id`    BIGINT UNSIGNED NOT NULL,
                    `channel_name` VARCHAR(64) NOT NULL,
                    `channel_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    PRIMARY KEY (`server_id`,`channel_name`),
                    FOREIGN KEY (`server_id`) REFERENCES servers (`server_id`))
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS `roles` (
                    `server_id` BIGINT UNSIGNED NOT NULL,
                    `role_name` VARCHAR(64) NOT NULL,
                    `role_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    PRIMARY KEY (`server_id`,`role_name`),
                    FOREIGN KEY (`server_id`) REFERENCES servers (`server_id`))
                    """);
            LOGGER.debug("Tables successfully validated.");
        } catch (SQLException e) {
            LOGGER.error("Failed to create tables. {}", e);
        } finally {
            close();
        }
    }

    @SneakyThrows
    public void validateServers() {
        if (!reconnect("validateServers")) return;
        Jarvis.getGuilds().forEach(guild -> addServer(guild.getIdLong()));
    }

    @SuppressWarnings("all")
    public boolean addServer(final long serverId) {
        return addServers(Set.of(serverId));
    }

    @SneakyThrows
    public boolean addServers(@NotNull final Collection<Long> ids) {
        if (!reconnect("addServers")) return false;
        LOGGER.debug("Adding {} new entr{}...", ids.size(), (ids.size() > 1 ? "ies" : "y"));
        try {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("INSERT IGNORE INTO `servers` VALUES (?)")) {
                for (final Long id : ids) {
                    if (id == null) continue;
                    statement.setLong(1, id);
                    statement.addBatch();
                    LOGGER.trace("[Server:{}] Added to batch.", id);
                }
                statement.executeBatch();
                LOGGER.trace("[Servers] Batch executed.");
            }
            try (var statement = connection.prepareStatement("INSERT IGNORE INTO `game_data_count` (`server_id`) VALUES (?)")) {
                for (final Long id : ids) {
                    if (id == null) continue;
                    statement.setLong(1, id);
                    statement.addBatch();
                    LOGGER.trace("[Server:{}] [Game:Count] Added to batch.", id);
                }
                statement.executeBatch();
                LOGGER.trace("[Servers] [Game:Count] Batch executed.");
            }
            try (var statement = connection.prepareStatement("INSERT IGNORE INTO `channels` (`server_id`,`channel_name`) VALUES (?,?)")) {
                for (final Long id : ids) {
                    if (id == null) continue;
                    statement.setLong(1, id);
                    for (final Channels.DevChannel channel : Channels.DevChannel.values()) {
                        statement.setString(2, channel.toString());
                        statement.addBatch();
                        LOGGER.trace("[Server:{}] [Channel:{}] Added to batch.", id, channel.toString());
                    }
                }
                statement.executeBatch();
                LOGGER.trace("[Servers] [Channels] Batch executed.");
            }
            try (var statement = connection.prepareStatement("INSERT IGNORE INTO `roles` (`server_id`,`role_name`) VALUES (?,?)")) {
                for (final Long id : ids) {
                    if (id == null) continue;
                    statement.setLong(1, id);
                    for (final Roles.DevRole role : Roles.DevRole.values()) {
                        statement.setString(2, role.toString());
                        statement.addBatch();
                        LOGGER.trace("[Server:{}] [Role:{}] Added to batch.", id, role.toString());
                    }
                }
                statement.executeBatch();
                LOGGER.trace("[Servers] [Roles] Batch executed.");
            }
            connection.commit();
            LOGGER.info("{} new entr{} added.", ids.size(), (ids.size() > 1 ? "ies" : "y"));
            return true;
        } catch (SQLException e) {
            connection.rollback();
            LOGGER.error("[Servers] Failed to add new entries. {}", e);
        } finally {
            connection.setAutoCommit(true);
        }
        return false;
    }

    public void removeServer(final long serverId) {
        if (!reconnect("addServer")) return;
        LOGGER.trace("[Server:{}] Removing entry...", serverId);
        try (var statement = connection.prepareStatement("DELETE FROM `servers` WHERE `server_id`=?")) {
            statement.setLong(1, serverId);
            final int rowsUpdated = statement.executeUpdate();
            LOGGER.info("[Server:{}] Entry removed. {} row{} affected.", serverId, rowsUpdated, (rowsUpdated > 1 ? "s" : ""));
        } catch (SQLException e) {
            LOGGER.error("[Server:{}] Failed to remove entry. {}", serverId, e);
        }
    }

    @Contract(pure = true)
    public boolean serverExists(final long serverId) {
        if (!reconnect("serverExists")) return true;
        LOGGER.trace("[Server:{}] Checking for entry...", serverId);
        try (var statement = connection.prepareStatement("SELECT * FROM `servers` WHERE `server_id`=?")) {
            statement.setLong(1, serverId);
            final ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                LOGGER.info("[Server:{}] No entry found.", serverId);
                return false;
            } else {
                LOGGER.info("[Server:{}] Entry found.", serverId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("[Server:{}] Failed to check if entry exists. {}", serverId, e);
        }
        return true;
    }

    public void addMember(@NotNull final CollectiveMember member) {
        addMember(member.server, member.id);
    }

    public void addMember(@NotNull final Member member) {
        addMember(member.getGuild().getIdLong(), member.getIdLong());
    }

    public void addMember(final long serverId, final long memberId) {
        if (memberExists(memberId, serverId)) return;
        LOGGER.trace("[Server:{}] [Member:{}] Adding new entry...", serverId, memberId);
        if (!reconnect("addMember")) return;
        try (var statement = connection.prepareStatement("INSERT IGNORE INTO `members` (`member_id`,`server_id`) VALUES (?,?)")) {
            statement.setLong(1, memberId);
            statement.setLong(2, serverId);
            final int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.debug("[Server:{}] [Member:{}] Failed to add. {} rows affected.", serverId, memberId, rowsAffected);
            } else LOGGER.debug("[Server:{}] [Member:{}] Successfully added. {} rows affected.", serverId, memberId, rowsAffected);
        } catch (Exception e) {
            LOGGER.error("[Server:{}] [Member:{}] Failed to add. {}", serverId, memberId, e);
        }
    }

    @Contract(pure = true)
    public boolean memberExists(final long memberId, final long serverId) {
        if (!reconnect("memberExists")) return true;
        LOGGER.trace("[Server:{}] [Member:{}] Checking for entry...", serverId, memberId);
        try (var statement = connection.prepareStatement("SELECT * FROM `members` WHERE `member_id`=? AND `server_id`=?")) {
            statement.setLong(1, memberId);
            statement.setLong(2, serverId);
            final ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                LOGGER.trace("[Server:{}] [Member:{}] No entry found.", serverId, memberId);
                if (!serverExists(serverId)) addServer(serverId);
                return false;
            } else {
                LOGGER.trace("[Server:{}] [Member:{}] Entry found.", serverId, memberId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("[Server:{}] [Member:{}] Failed to check if entry exists. {}", serverId, memberId, e);
        }
        return true;
    }

    @Contract(pure = true)
    public EnumMap<CollectiveMember.MemberData, Long> loadMember(final long serverId, final long memberId) {
        addMember(serverId, memberId);
        final EnumMap<CollectiveMember.MemberData, Long> data = new EnumMap<>(CollectiveMember.MemberData.class);
        if (!reconnect("loadMember")) return data;
        LOGGER.trace("[Server:{}] [Member:{}] Loading data...", serverId, memberId);
        try (var statement = connection.prepareStatement("SELECT * FROM `members` WHERE `member_id`=? AND `server_id`=?")) {
            statement.setLong(1, memberId);
            statement.setLong(2, serverId);
            final ResultSet result = statement.executeQuery();
            if (result.next()) {
                for (final CollectiveMember.MemberData type : CollectiveMember.MemberData.values()) {
                    data.put(type, result.getLong(type.toString()));
                }
                LOGGER.debug("[Server:{}] [Member:{}] Data loaded.", serverId, memberId);
            } else {
                LOGGER.debug("[Server:{}] [Member:{}] No data found.", serverId, memberId);
            }
        } catch (Exception e) {
            LOGGER.error("[Server:{}] [Member:{}] Failed to load data. {}", serverId, memberId, e);
        }
        return data;
    }

    public void saveMember(@NotNull final CollectiveMember member) {
        addMember(member);
        if (!reconnect("saveMember")) return;
        LOGGER.trace("[Server:{}] [Member:{}] Saving data...", member.server, member.id);
        try (var statement = connection.prepareStatement("UPDATE members SET `level`=?,`experience`=?,`correct_counts`=?,`incorrect_counts`=?,`images_sent`=?,`reactions_given`=?,`reactions_received`=? WHERE `member_id`=? AND `server_id`=?")) {
            final EnumMap<CollectiveMember.MemberData, Long> data = member.getCurrentData();
            statement.setLong(1, data.get(CollectiveMember.MemberData.LEVEL));
            statement.setLong(2, data.get(CollectiveMember.MemberData.EXPERIENCE));
            statement.setLong(3, data.get(CollectiveMember.MemberData.CORRECT_COUNTS));
            statement.setLong(4, data.get(CollectiveMember.MemberData.INCORRECT_COUNTS));
            statement.setLong(5, data.get(CollectiveMember.MemberData.IMAGES_SENT));
            statement.setLong(6, data.get(CollectiveMember.MemberData.REACTIONS_GIVEN));
            statement.setLong(7, data.get(CollectiveMember.MemberData.REACTIONS_RECEIVED));
            statement.setLong(8, member.id);
            statement.setLong(9, member.server);
            statement.executeUpdate();
            LOGGER.debug("[Server:{}] [Member:{}] Data saved.", member.server, member.id);
        } catch (Exception e) {
            LOGGER.error("[Server:{}] [Member:{}] Failed to save data. {}", member.server, member.id, e);
        }
    }

    @Contract(pure = true)
    public @NotNull EnumMap<Channels.DevChannel, Long> getChannelIds(final long serverId) {
        LOGGER.trace("[Server:{}] [Channels] Retrieving IDs...", serverId);
        final EnumMap<Channels.DevChannel, Long> channels = new EnumMap<>(Channels.DevChannel.class);
        if (!reconnect("getChannelIds")) return channels;
        try (var statement = connection.prepareStatement("SELECT `channel_name`,`channel_id` FROM `channels` WHERE `server_id`=?")) {
            statement.setLong(1, serverId);
            final ResultSet result = statement.executeQuery();
            while (result.next()) {
                channels.put(Channels.DevChannel.of(result.getString("channel_name")), result.getLong("channel_id"));
            }
            LOGGER.trace("[Server:{}] [Channels] {} ID{} found.", serverId, channels.size(), (channels.size() > 1 ? "s" : ""));
        } catch (SQLException e) {
            LOGGER.error("[Server:{}] [Channels] Failed to retrieve IDs. {}", serverId, e);
        }
        return channels;
    }

    @SneakyThrows
    public boolean saveChannelIds(final long serverId, @NotNull final EnumMap<Channels.DevChannel, Long> channels) {
        if (channels.isEmpty()) {
            LOGGER.debug("[Server:{}] Skipping update. No IDs found.", serverId);
            return false;
        } else if (!reconnect("saveChannelIds")) return false;
        LOGGER.debug("[Server:{}] [Channels] Updating {} ID{}...", serverId, channels.size(), (channels.size() > 1 ? "s" : ""));
        try {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("UPDATE `channels` SET `channel_id`=? WHERE `server_id`=? AND `channel_name`=?")) {
                statement.setLong(2, serverId);

                for (var index : channels.entrySet()) {
                    statement.setLong(1, index.getValue());
                    statement.setString(3, index.getKey().toString());
                    statement.addBatch();
                    LOGGER.debug("[Server:{}] [Channel:{}:{}] Added to batch.", serverId, index.getKey().toString(), index.getValue());
                }

                statement.executeBatch();
                LOGGER.debug("[Server:{}] [Channels] Batch executed.", serverId);
            }
            connection.commit();
            LOGGER.debug("[Server:{}] [Channels] {} ID{} updated.", serverId, channels.size(), (channels.size() > 1 ? "s" : ""));
            return true;
        } catch (SQLException e) {
            connection.rollback();
            LOGGER.error("[Server:{}] [Channels] Failed to update IDs. {}", serverId, e);
        } finally {
            connection.setAutoCommit(true);
        }
        return false;
    }

    public boolean updateChannelId(final long serverId, @NotNull final Channels.DevChannel channel, final long id) {
        if (!reconnect("updateChannelId")) return false;
        LOGGER.trace("[Server:{}] [Channel:{}:{}] Updating ID...", serverId, channel.toString(), id);
        try (var statement = connection.prepareStatement("UPDATE `channels` SET `channel_id`=? WHERE `server_id`=? AND `channel_name`=?")) {
            statement.setLong(1, id);
            statement.setLong(2, serverId);
            statement.setString(3, channel.toString());
            statement.executeUpdate();
            LOGGER.debug("[Server:{}] [Channel:{}:{}] ID updated.", serverId, channel.toString(), id);
            return true;
        } catch (SQLException e) {
            LOGGER.error("[Server:{}] [Channel:{}:{}] Failed to update ID. {}", serverId, channel.toString(), id, e);
        }
        return false;
    }

    @Contract(pure = true)
    public @NotNull EnumMap<Roles.DevRole, Long> getRoleIds(final long serverId) {
        LOGGER.trace("[Server:{}] [Roles] Retrieving IDs...", serverId);
        final EnumMap<Roles.DevRole, Long> roles = new EnumMap<>(Roles.DevRole.class);
        if (!reconnect("getRoleIds")) return roles;
        try (var statement = connection.prepareStatement("SELECT `role_name`,`role_id` FROM `roles` WHERE `server_id`=?")) {
            statement.setLong(1, serverId);
            final ResultSet result = statement.executeQuery();
            while (result.next()) {
                final Roles.DevRole role = Roles.DevRole.of(result.getString("role_name"));
                if (role != null) roles.put(role, result.getLong("role_id"));
            }
            LOGGER.trace("[Server:{}] [Roles] {} ID{} found.", serverId, roles.size(), (roles.size() > 1 ? "s" : ""));
        } catch (SQLException e) {
            LOGGER.error("[Server:{}] [Roles] Failed to retrieve IDs. {}", serverId, e);
        }
        return roles;
    }

    @SneakyThrows
    public boolean saveRoleIds(final long serverId, @NotNull final EnumMap<Roles.DevRole, Long> roles) {
        if (roles.isEmpty()) {
            LOGGER.trace("[Server:{}] [Roles] Skipping update. No IDs found.");
            return false;
        } else if (!reconnect("saveRoleIds")) return false;
        LOGGER.trace("[Server:{}] [Roles] Updating {} ID{}...", serverId, roles.size(), (roles.size() > 1 ? "s" : ""));
        try {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("UPDATE `roles` SET `role_id`=? WHERE `server_id`=? AND `role_name`=?")) {
                statement.setLong(2, serverId);

                for (var index : roles.entrySet()) {
                    statement.setLong(1, index.getValue());
                    statement.setString(3, index.getKey().toString());
                    statement.addBatch();
                    LOGGER.trace("[Server:{}] [Role:{}] Added to batch.", serverId, index.getValue());
                }

                statement.executeBatch();
                LOGGER.trace("[Server:{}] [Roles] Batch executed.", serverId);
            }
            connection.commit();
            LOGGER.debug("[Server:{}] [Roles] {} ID{} updated.", serverId, roles.size(), (roles.size() > 1 ? "s" : ""));
            return true;
        } catch (SQLException e) {
            connection.rollback();
            LOGGER.error("[Server:{}] [Roles] Failed to update IDs. {}", serverId, e);
        } finally {
            connection.setAutoCommit(true);
        }
        return false;
    }

    public boolean updateRoleId(final long serverId, @NotNull final Roles.DevRole role, final long id) {
        if (!reconnect("updateRoleId")) return false;
        LOGGER.trace("[Server:{}] [Role:{}:{}] Updating ID...", serverId, role.toString(), id);
        try (var statement = connection.prepareStatement("UPDATE `roles` SET `role_id`=? WHERE `server_id`=? AND `role_name`=?")) {
            statement.setLong(1, id);
            statement.setLong(2, serverId);
            statement.setString(3, role.toString());
            statement.executeUpdate();
            LOGGER.debug("[Server:{}] [Role:{}:{}] ID updated.", serverId, role.toString(), id);
            return true;
        } catch (SQLException e) {
            LOGGER.error("[Server:{}] [Role:{}:{}] Failed to update ID. {}", serverId, role.toString(), id, e);
        }
        return false;
    }

    public CountGameData getCountGameData(final long serverId) {
        final long channelId = Channels.getChannelId(serverId, Channels.DevChannel.COUNT);
        if (channelId == 0) {
            LOGGER.warn("[Server:{}] [Game:Count] No channel ID has been set yet.", serverId);
        } else if (reconnect("getCountGameData")) {
            LOGGER.trace("[Server:{}] [Game:Count] Loading data...", serverId);
            try (var statement = connection.prepareStatement("SELECT * FROM `game_data_count` WHERE `server_id`=?")) {
                statement.setLong(1, serverId);
                final ResultSet result = statement.executeQuery();
                if (result.next()) {
                    LOGGER.info("[Server:{}] [Game:Count] Data loaded.", serverId);
                    return new CountGameData(serverId,
                            channelId,
                            result.getLong("sync_message"),
                            result.getLong("latest_player"),
                            result.getInt("current_number"),
                            result.getBoolean("highscore_announced"),
                            result.getInt("highscore"),
                            result.getLong("highscore_epoch"));
                } else LOGGER.warn("[Server:{}] [Game:Count] No data found.", serverId);
            } catch (SQLException e) {
                LOGGER.error("[Server:{}] [Game:Count] Failed to load data. {}", serverId, e);
            }
        }
        return null;
    }

    public void saveCountGameData(@NotNull final CountGameData data) {
        if (!reconnect("saveCountGameData")) return;
        LOGGER.trace("[Server:{}] [Game:Count] Saving data...", data.serverId);
        try (var statement = connection.prepareStatement("UPDATE `game_data_count` SET `sync_message`=?,`latest_player`=?,`current_number`=?,`highscore_announced`=?,`highscore`=?,`highscore_epoch`=? WHERE `server_id`=?")) {
            statement.setLong(1, data.getSyncMessageId());
            statement.setLong(2, data.latestPlayer);
            statement.setInt(3, data.currentNumber);
            statement.setBoolean(4, data.highscoreAnnounced);
            statement.setInt(5, data.highscore);
            statement.setLong(6, data.highscoreEpoch);
            statement.setLong(7, data.serverId);
            statement.executeUpdate();
            LOGGER.debug("[Server:{}] [Game:Count] Data saved.", data.serverId);
        } catch (SQLException e) {
            LOGGER.error("[Server:{}] [Game:Count] Failed to save data. {}", data.serverId, e);
        }
        saveCurrentCountPlayers(data);
    }

    public HashMap<Long, CountPlayer> getCurrentCountPlayers(final long serverId) {
        final HashMap<Long, CountPlayer> players = HashMap.newHashMap(6);
        if (reconnect("getCurrentCountPlayers")) {
            LOGGER.trace("[Server:{}] [Game:Count] Loading players...", serverId);
            try (var statement = connection.prepareStatement("SELECT `member_id`,`counts` FROM `game_data` WHERE `server_id`=? AND NOT `counts` = 0 ORDER BY `counts` DESC")) {
                statement.setLong(1, serverId);
                final ResultSet result = statement.executeQuery();
                if (result.getFetchSize() > 0) LOGGER.debug("[Server:{}] [Game:Count] Players loaded.", serverId);
                else LOGGER.debug("[Server:{}] [Game:Count] No players found.", serverId);
                while (result.next()) {
                    final CountPlayer player = new CountPlayer(serverId, result.getLong("member_id"));
                    player.counts = result.getInt("counts");
                    players.put(player.id, player);
                }
            } catch (SQLException e) {
                LOGGER.error("[Server:{}] [Game:Count] Failed to load players. {}", serverId, e);
            }
        }
        return players;
    }

    @SneakyThrows
    public void saveCurrentCountPlayers(@NotNull final CountGameData data) {
        if (reconnect("saveCurrentCountPlayers")) {
            LOGGER.trace("[Server:{}] [Game:Count] Saving players...", data.serverId);
            if (data.getPlayers().isEmpty()) {
                try {
                    connection.createStatement().executeUpdate("UPDATE `game_data` SET `counts`=0 WHERE `server_id`=" + data.serverId);
                    LOGGER.debug("[Server:{}] [Game:Count] Players reset.", data.serverId);
                } catch (SQLException e) {
                    LOGGER.error("[Server:{}] [Game:Count] Failed to reset players. {}", data.serverId, e);
                }
            } else {
                connection.setAutoCommit(false);
                try (var statement = connection.prepareStatement("INSERT INTO `game_data` (`member_id`,`server_id`,`counts`) VALUES (?,?,?)" +
                                                                 "ON DUPLICATE KEY UPDATE `counts`=VALUES(`counts`)")) {
                    statement.setLong(2, data.serverId);

                    for (final CountPlayer player : data.getPlayers()) {
                        statement.setLong(1, player.id);
                        statement.setLong(3, player.counts);
                        statement.addBatch();
                    }
                    statement.executeBatch();
                    connection.commit();
                    LOGGER.debug("[Server:{}] [Game:Count] Players saved.", data.serverId);
                } catch (SQLException e) {
                    connection.rollback();
                    LOGGER.error("[Server:{}] [Game:Count] Failed to save players. {}", data.serverId, e);
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        }
    }
}