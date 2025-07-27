package dev.prodzeus.jarvis.games.count.game;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.configuration.Channels;
import dev.prodzeus.jarvis.enums.ServerCount;
import dev.prodzeus.jarvis.games.count.CountGameHandler;
import dev.prodzeus.jarvis.member.CollectiveMember;
import dev.prodzeus.jarvis.member.MemberManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static dev.prodzeus.jarvis.bot.Jarvis.LOGGER;
import static dev.prodzeus.jarvis.games.count.CountGameHandler.formatPercentage;

public class CountGame {

    private final boolean enabled;
    private final long serverId;
    private final long channelId;
    private final TextChannel channel;

    private long latestPlayer = 0L;
    private int currentNumber;
    private final Map<Long,CountPlayer> counts = new HashMap<>();

    private boolean highscoreAnnounced = false;
    private int highscore;
    private long timeOfHighscore;

    private final List<CountPlayer> countPlayers = new ArrayList<>();

    public CountGame(final long serverId) {
        LOGGER.debug("New Count Game created for server {}.",serverId);
        this.serverId = serverId;
        this.channelId = Channels.get(serverId).countChannel;
        if (this.channelId == 0) {
            this.channel = null;
            LOGGER.error("Could not find channel for server {}! Are Channel IDs registered?", serverId);
            CountGameHandler.removeGame(serverId);
        } else this.channel = Jarvis.BOT.jda.getTextChannelById(channelId);
        final ServerCount count = Jarvis.DATABASE.getServerCountStats(serverId);
        this.currentNumber = count.current();
        this.highscore = count.highscore();
        this.timeOfHighscore = count.epochTime();
        enabled = channel != null;
    }

    public void save() {
        Jarvis.DATABASE.saveServerCountStats(new ServerCount(serverId, currentNumber, highscore, timeOfHighscore));
        Jarvis.LOGGER.info("Count data saved to database for server {}!",serverId);
    }

    public void run(@NotNull final MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() == channelId){
            if (!enabled) {
                LOGGER.warn("Count Game is disabled for server {}, due to errors. Ignoring message event.",serverId);
                return;
            }
        } else return;

        final Message message = event.getMessage();

        deleteMessage(message);

        int countedNumber;
        try {
            countedNumber = Integer.parseInt(message.getContentRaw(), 10);
        } catch (Exception e) {
            if (!(e instanceof NumberFormatException)) LOGGER.warn("Failed to parse message to Integer in count! {}", e);
            return;
        }

        final CollectiveMember collectiveMember = MemberManager.getCollectiveMember(
                event.getMember().getIdLong(),
                event.getGuild().getIdLong());

        if (!canPlay(collectiveMember)) return;

        String text;
        if (countedNumber == currentNumber++) {
            deleteWarningMessage(channel);
            collectiveMember.incrementCorrectCounts();
            counts.computeIfAbsent(collectiveMember.id,CountPlayer::new).counts++;
            final String streak = computeStreak(event) ? " " + CountGameHandler.streak : "";
            text = "## " + collectiveMember.mention + " **"
                   + countedNumber + "** "
                   + (countedNumber > highscore ? CountGameHandler.trophy : streak) + "\n-# "
                   + (streak.isEmpty() ? collectiveMember.getCountLevel() : (countedNumber > highscore ? streak : "")) + " •  "
                   + CountGameHandler.correctCountEmoji + " **"
                   + collectiveMember.getCorrectCounts() + "**  •  "
                   + CountGameHandler.incorrectCountEmoji + " **"
                   + collectiveMember.getIncorrectCounts() + "**";
            if (countedNumber > highscore) {
                if (!highscoreAnnounced) {
                    highscoreAnnounced = true;
                    text = text + CountGameHandler.newHighscoreText.formatted(collectiveMember.mention, countedNumber, highscore, timeOfHighscore);
                }
                highscore = countedNumber;
                timeOfHighscore = event.getMessage().getTimeCreated().toEpochSecond();
            }
        } else {
            currentNumber--;
            //if (currentNumber == 1) return;
            collectiveMember.incrementIncorrectCounts();
            try {
                Collections.sort(countPlayers);
            } catch (Exception e) {
                LOGGER.warn("Attempted to sort list of count players but failed! {}",e);
                return;
            }
            final String scoreText = highscoreAnnounced ? "New Highscore" : "Score";
            final String scoreEmoji = highscoreAnnounced ? CountGameHandler.trophy : "";
            final Leaderboard leaderboard = new Leaderboard(counts);
            final CountPlayer firstPlace = leaderboard.removeFirst();
            try {
                if (firstPlace != null) {
                    final CountPlayer secondPlace = leaderboard.removeFirst();
                    if (secondPlace != null) {
                        final CountPlayer thirdPlace = leaderboard.removeFirst();
                        if (thirdPlace != null) {
                            text = CountGameHandler.gameOverText.formatted(scoreText, currentNumber, scoreEmoji, collectiveMember.mention, "<@" + firstPlace.id + ">", firstPlace.counts, formatPercentage(currentNumber,firstPlace.counts),
                                    "<@" + secondPlace.id + ">", secondPlace.counts, formatPercentage(currentNumber,secondPlace.counts),
                                    "<@" + thirdPlace.id + ">", thirdPlace.counts, formatPercentage(currentNumber,thirdPlace.counts), countPlayers.size());
                        } else {
                            text = CountGameHandler.gameOverText.formatted(scoreText, currentNumber, scoreEmoji, collectiveMember.mention, "<@" + firstPlace.id + ">", firstPlace.counts, formatPercentage(currentNumber,firstPlace.counts),
                                    "<@" + secondPlace.id + ">", secondPlace.counts, formatPercentage(currentNumber,secondPlace.counts),
                                    "N/A", "N/A", "N/A", 0);
                        }
                    } else {
                        text = CountGameHandler.gameOverText.formatted(scoreText, currentNumber, scoreEmoji, collectiveMember.mention, "<@" + firstPlace.id + ">", firstPlace.counts, formatPercentage(currentNumber,firstPlace.counts),
                                "N/A", "N/A", "N/A",
                                "N/A", "N/A", "N/A", 0);
                    }
                } else text = CountGameHandler.gameOverText.formatted(scoreText, currentNumber, scoreEmoji, collectiveMember.mention, "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", 0);
            } catch (Exception e) {
                LOGGER.error("Attempted to format GameOver string for count but failed! {}",e);
                return;
            }
            if (highscoreAnnounced) {
                highscoreAnnounced = false;
                channel.getManager()
                        .setTopic("Server Highscore: %d".formatted(highscore))
                        .queue(null, f -> LOGGER.warn("RestAction failed! Attempted to send new highscore message, and update topic to new highscore, but failed. {}", f));
            }
            reset();
        }
        try {
            channel.sendMessage(text).queue(null, f -> LOGGER.error("Failed to send game message in count! {}", f));
        } catch (Exception e) {
            LOGGER.error("Failed to send game message in count! {}", e);
        }
    }

    private boolean canPlay(final CollectiveMember member) {
        if (latestPlayer == member.id) {
            if (warningMessage == 0L) {
                channel.sendMessage("%s You can't count twice in a row!".formatted(Jarvis.BOT.getEmojiFormatted("red_exclamation")))
                        .queue(s -> {
                            warningMessage = s.getIdLong();
                            s.delete().queueAfter(10, TimeUnit.SECONDS, su -> warningMessage = 0L);
                        });
            }
            return false;
        } else {
            latestPlayer = member.id;
            return true;
        }
    }

    private long warningMessage = 0L;
    private synchronized void deleteWarningMessage(@NotNull final TextChannel channel) {
        if (warningMessage != 0L) {
            try {
                channel.deleteMessageById(warningMessage).submit().thenAccept(s -> warningMessage = 0L);
            } catch (Exception ignored) {}
        }
    }

    private final Map<Long, List<Long>> playerStreaks = new HashMap<>();
    private boolean computeStreak(@NotNull final MessageReceivedEvent event) {
        final List<Long> times = playerStreaks.computeIfAbsent(event.getMember().getIdLong(), k -> new ArrayList<>());
        final long compareTime = event.getMessage().getTimeCreated().toEpochSecond();
        times.add(compareTime);
        if (times.size() < 6) return false;
        times.removeFirst();
        return times.stream().allMatch(time -> (time + 60000) > compareTime);
    }

    private static class Leaderboard extends ArrayList<CountPlayer> {
        public Leaderboard(final Map<Long,CountPlayer> players) {
            super(players.values());
            Collections.sort(this);
        }
    }

    private void deleteMessage(final Message message) {
        try {
            message.delete().queueAfter(300, TimeUnit.MILLISECONDS,
                    null,
                    f -> Jarvis.LOGGER.warn("Failed to delete message in count channel! {}", f));
        } catch (Exception e) { Jarvis.LOGGER.warn("Failed to delete message in count channel! {}", e); }
    }

    private void reset() {
        currentNumber = 1;
        save();
        playerStreaks.clear();
        countPlayers.clear();
    }
}
