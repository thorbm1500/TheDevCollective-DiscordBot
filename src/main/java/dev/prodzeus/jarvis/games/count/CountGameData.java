package dev.prodzeus.jarvis.games.count;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.games.components.GameData;
import dev.prodzeus.jarvis.member.CollectiveMember;
import dev.prodzeus.jarvis.member.MemberManager;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;

public final class CountGameData extends GameData {

    private final Logger logger = SLF4JProvider.get().getLogger("Count:" + serverId);

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

        sync();
    }

    private void sync() {
        if (previousSync != 0) {
            logger.trace("Deleting previous sync message: {}...", previousSync);
            try {
                channel.deleteMessageById(previousSync)
                        .reason("Deleting previous Sync Message before sending new Sync Message.")
                        .queue(s -> {
                            previousSync = 0;
                            logger.trace("Previous sync message deleted.");
                        },
                        f -> {
                            if (f instanceof ErrorResponseException error) {
                                logger.error("Failed to delete previous sync message! {}", error.getMeaning());
                            } else logger.error("Failed to delete previous sync message! {}", f);
                        }
                        );
            } catch (Exception e) {
                logger.warn("Failed to delete previous sync message! {}  ", e);
            }
        }
        logger.trace("Sending new sync message...");
        try {
            channel.sendMessage("## %s Game Sync\nNext number: **%d**"
                            .formatted(Jarvis.getEmojiFormatted("sync"), currentNumber))
                    .queue(s -> {
                                currentSync = s.getIdLong();
                                logger.trace("New sync message: {}", currentSync);
                            },
                            f -> {
                                if (f instanceof ErrorResponseException error) {
                                    logger.error("Failed to delete current sync message! {}", error.getMeaning());
                                } else logger.error("Failed to delete current sync message! {}", f);
                            });
        } catch (Exception e) {
            logger.warn("Failed to send new sync message! {}", e);
        }
    }

    private void deleteCurrentSyncMessage() {
        if (currentSync != 0) {
            try {
                logger.debug("Deleting current sync message: {}...", currentSync);
                channel.deleteMessageById(currentSync).queue(s -> {
                            currentSync = 0;
                            logger.debug("Current sync message deleted.");
                        },
                        f -> logger.warn("Failed to delete current sync message! {}  ", f));
            } catch (Exception e) {
                logger.warn("Failed to delete current sync message! {}  ", e);
            }
        }
    }


    @Contract(pure = true)
    public @NotNull CountPlayer getPlayer(final long id) {
        logger.trace("Getting Count Player: {}", id);
        if (players.containsKey(id)) {
            logger.trace("Returning existing Count Player.");
            return players.get(id);
        } else {
            logger.trace("Creating new Count Player.");
            final CountPlayer player = new CountPlayer(serverId, id);
            synchronized (players) {
                players.put(id, player);
            }
            return player;
        }
    }

    @Contract(pure = true)
    public synchronized boolean handleCount(@NotNull final MessageReceivedEvent event, final int counted) {
        if (counted == currentNumber) {
            latestPlayer = event.getAuthor().getIdLong();
            deleteCurrentSyncMessage();
            if (currentNumber > highscore && !highscoreAnnounced) {
                highscoreAnnounced = true;
                channel.sendMessage(CountGameHandler.newHighscoreText.formatted(event.getAuthor().getAsMention(), currentNumber, highscore, highscoreEpoch)).queue();
                highscore = currentNumber;
                highscoreEpoch = event.getMessage().getTimeCreated().toEpochSecond();
            }
            incrementCount();
            return true;
        } else {
            getPlayer(latestPlayer).wrongCount = true;
            MemberManager.getCollectiveMember(serverId, latestPlayer).increment(CollectiveMember.MemberData.INCORRECT_COUNTS);
            return false;
        }
    }

    private void incrementCount() {
        logger.trace("Next Number: {}", ++currentNumber);
        getPlayer(latestPlayer).incrementCount();
        MemberManager.getCollectiveMember(serverId, latestPlayer).increment(CollectiveMember.MemberData.CORRECT_COUNTS);
        logger.trace("Collective Member Count incremented: {}", latestPlayer);
    }

    @Contract(pure = true)
    public @NotNull Collection<CountPlayer> getPlayers() {
        return players.values();
    }

    public void resetPlayers() {
        synchronized (players) {
            players.clear();
        }
    }

    @Contract(pure = true)
    public @NotNull CountGame.FinishedGameStats getGameStats() {
        return leaderboard == null ? new CountGame.FinishedGameStats(this) : leaderboard;
    }

    @Override
    public void save() {
        if (highscoreAnnounced) channel.getManager().setTopic("Server Highscore: " + currentNumber).queue();
        Jarvis.DATABASE.saveCountGameData(this);
        logger.info("Count data saved to database.");
    }

    @Override
    public void reset() {
        resetPlayers();
        currentNumber = 1;
        highscoreAnnounced = false;
    }
}
