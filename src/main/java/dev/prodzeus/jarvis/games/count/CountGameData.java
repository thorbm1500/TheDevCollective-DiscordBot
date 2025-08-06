package dev.prodzeus.jarvis.games.count;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.games.components.GameData;
import dev.prodzeus.jarvis.member.CollectiveMember;
import dev.prodzeus.jarvis.member.MemberManager;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.Marker;
import dev.prodzeus.logger.SLF4JProvider;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;

public final class CountGameData extends GameData {

    private static final Logger logger = SLF4JProvider.get().getLogger("Count");
    private final Marker marker;

    public long latestPlayer;
    public int currentNumber;
    private CountGame.FinishedGameStats leaderboard;

    public boolean highscoreAnnounced;
    public int highscore;
    public long highscoreEpoch;

    private long previousSync;
    private long currentSync = 0;

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
                channel.deleteMessageById(previousSync)
                        .reason("Deleting previous Sync Message before sending new Sync Message.")
                        .queue(s -> logger.trace(marker,"Previous sync message deleted."),
                                f -> logger.debug(marker,"Previous sync message failed to delete."));
                previousSync = 0;
            } catch (Exception e) {
                logger.error(marker,"Failed to delete previous sync message! {}  ", e);
            }
        }
        logger.trace(marker,"Sending new sync message...");
        try {
            channel.sendMessage("## %s Game Sync\nNext number: **%d**".formatted(Jarvis.getEmojiFormatted("sync"), currentNumber))
                    .queue(s -> logger.trace(marker,"New sync message: {}", currentSync = s.getIdLong()));
        } catch (Exception e) {
            logger.warn(marker,"Failed to send new sync message! {}", e);
        }
    }

    private boolean queued = false;
    private void deleteCurrentSyncMessage() {
        if (currentSync != 0 && !queued) {
            try {
                logger.debug(marker,"Deleting current sync message: {}...", currentSync);
                channel.deleteMessageById(currentSync).queue(s -> { currentSync = 0;
                            logger.debug(marker,"Current sync message deleted."); },
                        f -> logger.warn(marker,"Failed to delete current sync message! {}  ", f));
                queued = true;
            } catch (Exception e) {
                logger.warn(marker,"Failed to delete current sync message! {}  ", e);
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
            synchronized (players) {
                logger.trace(marker,"New Count Player cached. Previous value connected to ID: {}", players.put(id, player));
            }
            return player;
        }
    }

    @Contract(pure = true)
    public synchronized boolean handleCount(@NotNull final MessageReceivedEvent event, final int counted) {
        if (counted == currentNumber) {
            latestPlayer = event.getAuthor().getIdLong();
            logger.trace(marker,"Player: {} | Count: {} | Correct: True",latestPlayer,counted);
            deleteCurrentSyncMessage();
            if (currentNumber > highscore && !highscoreAnnounced) {
                highscoreAnnounced = true;
                channel.sendMessage(CountGameHandler.newHighscoreText.formatted(event.getAuthor().getAsMention(), currentNumber, highscore, highscoreEpoch)).queue();
                highscore = currentNumber;
                highscoreEpoch = event.getMessage().getTimeCreated().toEpochSecond();
            }
            incrementCount();
            logger.trace(marker,"Next Number: {}", ++currentNumber);
            return true;
        } else {
            logger.trace(marker,"Player: {} | Count: {} | Correct: False",latestPlayer,counted);
            getPlayer(latestPlayer).wrongCount = true;
            MemberManager.getCollectiveMember(serverId, latestPlayer).increment(CollectiveMember.MemberData.INCORRECT_COUNTS);
            return false;
        }
    }

    private void incrementCount() {
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
