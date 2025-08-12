package dev.prodzeus.jarvis.games.count;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.games.components.GameData;
import dev.prodzeus.jarvis.member.CollectiveMember;
import dev.prodzeus.jarvis.member.MemberManager;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Marker;

import java.util.Collection;
import java.util.HashMap;

public final class CountGameData extends GameData {

    private static final Logger logger = SLF4JProvider.get().getLoggerFactory().getLogger("Count");
    private final Marker marker;

    public long latestPlayer;
    public int currentNumber;
    private CountGame.FinishedGameStats leaderboard;

    public boolean highscoreAnnounced;
    public int highscore;
    public long highscoreEpoch;

    private volatile long previousSync;
    private volatile long currentSync = 0;

    public long getSyncMessageId() {
        return currentSync;
    }

    private final HashMap<Long, CountPlayer> players = HashMap.newHashMap(6);

    public CountGameData(final long serverId, final long channelId, final long syncMessageId, final long latestPlayerId, final int currentNumber, final boolean highscoreAnnounced, final int highscore, final long highscoreEpoch) {
        super(serverId, channelId);
        this.previousSync = syncMessageId;
        this.latestPlayer = latestPlayerId;
        this.currentNumber = currentNumber;
        this.highscoreAnnounced = highscoreAnnounced;
        this.highscore = highscore;
        this.highscoreEpoch = highscoreEpoch;
        final HashMap<Long, CountPlayer> cachedPlayers = Jarvis.DATABASE.getCurrentCountPlayers(serverId);
        if (cachedPlayers != null && !cachedPlayers.isEmpty()) players.putAll(cachedPlayers);
        this.marker = SLF4JProvider.get().getMarkerFactory().getMarker(String.valueOf(serverId));

        sync();
    }

    private void sync() {
        if (previousSync != 0) {
            logger.trace(marker,"Deleting previous sync message: {}...", previousSync);
            try {
                final Message message = channel.getHistory().getMessageById(previousSync);
                if (message == null) return;
                logger.debug(marker,"Deleting current sync message: {}...", currentSync);
                message.delete().reason("Deleting current sync message.").queue();
                previousSync = 0;
            } catch (ErrorResponseException e) {
                logger.debug(marker,"Previous sync message failed to delete. {}", e.getMeaning());
            }
        }
        logger.trace(marker,"Sending new sync message...");
        try {
            channel.sendMessage("## %s Game Sync\nNext number: **%d**".formatted(Jarvis.getEmojiFormatted("sync"), currentNumber))
                    .queue(s -> logger.trace(marker,"New sync message: {}", currentSync = s.getIdLong()));
        } catch (Exception ignored) {
            logger.debug(marker,"Previous sync message failed to delete.");
        }
    }

    private volatile boolean queued = false;
    private void deleteCurrentSyncMessage() {
        if (currentSync != 0 && !queued) {
            try {
                final Message message = channel.getHistory().getMessageById(currentSync);
                if (message == null) return;
                logger.debug(marker,"Deleting previous sync message: {}...", currentSync);
                message.delete().reason("Deleting previous sync Message before sending new sync message.").queue();
                queued = true;
            } catch (Exception ignored) {
                logger.debug(marker,"Previous sync message failed to delete.");
            }
        }
    }


    @Contract(pure = true)
    public @NotNull CountPlayer getPlayer(final long id) {
        logger.trace(marker,"Getting Count Player: {}", id);
        if (players.containsKey(id)) {
            logger.trace(marker,"Returning existing Count Player.");
            return players.get(id);
        } else {
            logger.trace(marker,"Creating new Count Player.");
            final CountPlayer player = new CountPlayer(serverId, id);
            var value = players.put(id, player);
            synchronized (players) {
                logger.trace(marker,"New Count Player cached. Previous value connected to ID: {}", value == null ? "null" : value);
            }
            return player;
        }
    }

    @SneakyThrows @Contract(pure = true)
    public synchronized boolean handleCount(@NotNull final MessageReceivedEvent event, final int counted) {
        latestPlayer = event.getAuthor().getIdLong();
        deleteCurrentSyncMessage();
        if (counted == currentNumber) {
            logger.trace(marker,"[Member:{}] {} | Correct",latestPlayer,counted);
            if (currentNumber > highscore && !highscoreAnnounced) {
                highscoreAnnounced = true;
                channel.sendMessage(CountGameHandler.newHighscoreText.formatted(event.getAuthor().getAsMention(), currentNumber, highscore, highscoreEpoch)).queue();
                highscore = currentNumber;
                highscoreEpoch = event.getMessage().getTimeCreated().toEpochSecond();
            }
            incrementCount();
            logger.trace(marker,"Next Number: {}", currentNumber);
            return true;
        } else {
            logger.trace(marker,"[Member:{}] {} | Incorrect",latestPlayer,counted);
            getPlayer(latestPlayer).wrongCount = true;
            MemberManager.getCollectiveMember(serverId, latestPlayer).increment(CollectiveMember.MemberData.INCORRECT_COUNTS);
            return false;
        }
    }

    private void incrementCount() {
        currentNumber++;
        getPlayer(latestPlayer).incrementCount();
        MemberManager.getCollectiveMember(serverId, latestPlayer).increment(CollectiveMember.MemberData.CORRECT_COUNTS);
        logger.trace(marker,"Collective Member Count incremented: {}", latestPlayer);
    }

    @Contract(pure = true)
    public @NotNull Collection<CountPlayer> getPlayers() {
        return players.values();
    }

    public void resetPlayers() {
        logger.trace(marker,"Resetting players...");
        synchronized (players) {
            players.clear();
        }
    }

    @Contract(pure = true)
    public @NotNull CountGame.FinishedGameStats getGameStats() {
        return leaderboard == null ? leaderboard = new CountGame.FinishedGameStats(this) : leaderboard;
    }

    @Override
    public void save() {
        if (highscoreAnnounced) channel.getManager().setTopic("Server Highscore: " + currentNumber).queue(null,null);
        Jarvis.DATABASE.saveCountGameData(this);
        logger.info(marker,"Count data saved to database.");
    }

    @Override
    public void reset() {
        resetPlayers();
        currentNumber = 1;
        highscoreAnnounced = false;
    }
}
