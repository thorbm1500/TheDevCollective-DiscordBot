package dev.prodzeus.jarvis.games.count;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.games.components.GameData;
import dev.prodzeus.jarvis.member.CollectiveMember;
import dev.prodzeus.jarvis.member.MemberManager;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import org.jetbrains.annotations.Contract;

import java.util.Collection;
import java.util.HashMap;

public class CountGameData extends GameData {

    private final Logger logger = SLF4JProvider.get().getLogger("Count:"+serverId);

    public long syncMessage;
    public long latestPlayer;
    public int currentNumber;
    private CountGame.Leaderboard leaderboard;

    public boolean highscoreAnnounced;
    public int highscore;
    public long highscoreEpoch;

    private boolean synced = false;

    private final HashMap<Long,CountPlayer> players = HashMap.newHashMap(6);

    public CountGameData(final long serverId, final long channelId, final long syncMessage, final long latestPlayer, final int currentNumber, final boolean highscoreAnnounced, final int highscore, final long highscoreEpoch) {
        super(serverId, channelId);
        this.syncMessage = syncMessage;
        this.latestPlayer = latestPlayer;
        this.currentNumber = currentNumber;
        this.highscoreAnnounced = highscoreAnnounced;
        this.highscore = highscore;
        this.highscoreEpoch = highscoreEpoch;
        final HashMap<Long,CountPlayer> cachedPlayers = Jarvis.DATABASE.getCurrentCountPlayers(serverId);
        if (cachedPlayers != null && !cachedPlayers.isEmpty()) players.putAll(cachedPlayers);

        sync();
    }

    private void sync() {
        deleteSyncMessage();
        try {
            logger.debug("Sending sync message...");
            channel.sendMessage("## %s Game Sync\nNext number: **%d**"
                            .formatted(Jarvis.getEmojiFormatted("sync"), currentNumber))
                    .queue(s -> {
                        syncMessage = s.getIdLong();
                        synced = false;
                        logger.debug("New sync message: {}",syncMessage);
                    },
                            f -> logger.warn("Failed to send sync message! {}  ", f));
        } catch (Exception e) {
            logger.warn("Failed to send sync message! {}", e);
        }
    }

    private void deleteSyncMessage() {
        if (synced || syncMessage == 0) return;
        try {
            logger.debug("Deleting sync message: {}...", syncMessage);
            channel.deleteMessageById(syncMessage).queue(s -> logger.debug("Sync message deleted."),
                    f -> logger.warn("Failed to delete sync message! {}  ", f));
        } catch (Exception e) {
            logger.warn("Failed to delete sync message! {}  ", e);
        }
        syncMessage = 0;
        synced = true;
    }

    @Contract(pure = true)
    public CountPlayer getPlayer(final long id) {
        try {
            logger.trace("Getting Count Player: {}",id);
            if (players.containsKey(id)) {
                logger.trace("Returning existing Count Player.");
                return players.get(id);
            }
            else {
                logger.trace("Creating new Count Player.");
                final CountPlayer player = new CountPlayer(serverId, id);
                players.put(id, player);
                return player;
            }
        } catch (Exception e) {
            logger.error("Failed to get count player! {}", e);
        }
        return null;
    }

    @Contract(pure = true)
    public synchronized boolean handleCount(final long memberId, final int counted) {
        if (counted == currentNumber) {
            latestPlayer = memberId;
            deleteSyncMessage();
            if (isNewHighscore() && !highscoreAnnounced) {
                highscoreAnnounced = true;
                channel.sendMessage(CountGameHandler.newHighscoreText.formatted("<@"+memberId+">", currentNumber, highscore, highscoreEpoch)).queue();
                highscore = currentNumber;
                highscoreEpoch = System.currentTimeMillis();
            }
            incrementCount();
            return true;
        } else {
            wrongCount();
            save();
            return false;
        }
    }

    @Contract(pure = true)
    private boolean isNewHighscore() {
        return currentNumber > highscore;
    }

    private void incrementCount() {
        currentNumber++;
        logger.trace("Next Number: {}", currentNumber);
        getPlayer(latestPlayer).incrementCount();
        logger.trace("Player Count incremented: {}", latestPlayer);
        MemberManager.getCollectiveMember(serverId,latestPlayer).increment(CollectiveMember.MemberData.CORRECT_COUNTS);
        logger.trace("Collective Member Count incremented: {}", latestPlayer);
    }

    private void wrongCount() {
        getPlayer(latestPlayer).wrongCount = true;
        leaderboard = new CountGame.Leaderboard(this);
        MemberManager.getCollectiveMember(serverId,latestPlayer).increment(CollectiveMember.MemberData.INCORRECT_COUNTS);
    }

    @Contract(pure = true)
    public Collection<CountPlayer> getPlayers() {
        return players.values();
    }

    public void resetPlayers() {
        players.clear();
    }

    @Contract(pure = true)
    public CountGame.Leaderboard getLeaderboard() {
        return leaderboard == null ? new CountGame.Leaderboard(this) : leaderboard;
    }

    @Override
    public void save() {
        if (highscoreAnnounced) channel.getManager().setTopic("Server Highscore: " + currentNumber).queue();
        Jarvis.DATABASE.saveCountGameData(this);
    }

    @Override
    public void reset() {
        resetPlayers();
        currentNumber = 1;
        highscoreAnnounced = false;
    }
}
