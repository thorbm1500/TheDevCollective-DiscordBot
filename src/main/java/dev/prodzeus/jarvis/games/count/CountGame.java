package dev.prodzeus.jarvis.games.count;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.member.CollectiveMember;
import dev.prodzeus.jarvis.member.MemberManager;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static dev.prodzeus.jarvis.games.count.CountGameHandler.formatPercentage;

@Slf4j
public class CountGame extends ListenerAdapter {

    private final Logger logger;

    private final CountGameData data;

    public CountGame(final long serverId) {
        logger = SLF4JProvider.get().getLogger("Count:" + serverId);
        logger.debug("New instance created!", serverId);

        this.data = Jarvis.DATABASE.getCountGameData(serverId);
        if (data == null) {
            logger.debug("[Server:{}] [Game:Count] Removing game instance...", serverId);
            return;
        }

        CountGameHandler.addGame(serverId, this);
    }

    public void save() {
        data.save();
        logger.info("Count data saved to database for server {}!", data.serverId);
    }

    @Override
    public void onMessageReceived(@NotNull final MessageReceivedEvent event) {
        if (event.isWebhookMessage()
            || event.getAuthor().isBot() || event.getAuthor().isSystem()
            || event.getChannel().getIdLong() != data.channel.getIdLong()) return;

        final Message message = event.getMessage();
        deleteMessage(message);

        int countedNumber;
        try {
            logger.trace("Checking for integer...");
            countedNumber = Integer.parseInt(message.getContentRaw(), 10);
        } catch (Exception e) {
            logger.trace("No integer found.");
            if (!(e instanceof NumberFormatException)) logger.warn("Failed to parse message to Integer in count! {}", e);
            return;
        }
        logger.trace("Integer {} found.", countedNumber);

        final long memberId = event.getAuthor().getIdLong();
        if (!canPlay(memberId)) return;

        String text = "";
        if (data.handleCount(memberId, countedNumber)) {
            try {
                deleteWarningMessage();
                final String streak = computeStreak(event) ? " " + CountGameHandler.streak : "";
                final CollectiveMember collectiveMember = MemberManager.getCollectiveMember(data.serverId, memberId);
                text = "### <@" + data.latestPlayer + "> **"
                       + countedNumber + "** "
                       + (data.highscoreAnnounced ? CountGameHandler.trophy : streak) + "\n-# "
                       + (streak.isEmpty() ? collectiveMember.getCountLevelIcon() : (data.highscoreAnnounced ? streak : "")) + " •  "
                       + CountGameHandler.correctCountEmoji + " **"
                       + collectiveMember.getData(CollectiveMember.MemberData.CORRECT_COUNTS) + "**  "
                       + CountGameHandler.incorrectCountEmoji + " **"
                       + collectiveMember.getData(CollectiveMember.MemberData.INCORRECT_COUNTS) + "**  •  "
                       + CountGameHandler.beta;
            } catch (Exception e) {
                logger.error("Failed to handle count! {}", e);
            }
        } else {
            //if (currentNumber == 1) return;
            final String scoreText = data.highscoreAnnounced ? "New Highscore" : "Score";
            final String scoreEmoji = data.highscoreAnnounced ? CountGameHandler.trophy : "";
            final Leaderboard leaderboard = data.getLeaderboard();
            try {
                if (!leaderboard.isEmpty()) {
                    final CountPlayer firstPlace = leaderboard.removeFirst();
                    if (!leaderboard.isEmpty()) {
                        final CountPlayer secondPlace = leaderboard.removeFirst();
                        if (!leaderboard.isEmpty()) {
                            final CountPlayer thirdPlace = leaderboard.removeFirst();
                            text = CountGameHandler.gameOverText.formatted(scoreText, data.currentNumber, scoreEmoji, "<@" + firstPlace.id + ">",
                                    "<@" + firstPlace.id + ">", firstPlace.experienceGained, firstPlace.counts, formatPercentage(data.currentNumber, firstPlace.counts),
                                    "<@" + secondPlace.id + ">", secondPlace.experienceGained, secondPlace.counts, formatPercentage(data.currentNumber, secondPlace.counts),
                                    "<@" + thirdPlace.id + ">", thirdPlace.experienceGained, thirdPlace.counts, formatPercentage(data.currentNumber, thirdPlace.counts), leaderboard.size());
                        } else {
                            text = CountGameHandler.gameOverText.formatted(scoreText, data.currentNumber, scoreEmoji, "<@" + firstPlace.id + ">",
                                    "<@" + firstPlace.id + ">", firstPlace.experienceGained, firstPlace.counts, formatPercentage(data.currentNumber, firstPlace.counts),
                                    "<@" + secondPlace.id + ">", secondPlace.experienceGained, secondPlace.counts, formatPercentage(data.currentNumber, secondPlace.counts),
                                    "N/A", 0, "N/A", "N/A", 0);
                        }
                    } else {
                        text = CountGameHandler.gameOverText.formatted(scoreText, data.currentNumber, scoreEmoji, "<@" + firstPlace.id + ">",
                                "<@" + firstPlace.id + ">", firstPlace.experienceGained, firstPlace.counts, formatPercentage(data.currentNumber, firstPlace.counts),
                                "N/A", 0, "N/A",
                                "N/A", 0, "N/A", 0);
                    }
                } else text = CountGameHandler.gameOverText.formatted(scoreText, data.currentNumber, scoreEmoji, "<@" + data.latestPlayer + ">",
                        "N/A", 0, "N/A",
                        "N/A", 0, "N/A",
                        "N/A", 0, "N/A", 0);
            } catch (Exception e) {
                logger.error("Attempted to format GameOver string for count but failed! {}", e);
                return;
            }
            reset();
        }
        try {
            data.channel.sendMessage(text).queue(s -> logger.trace("New message sent in count."), f -> logger.error("Failed to send game message in count! {}", f));
        } catch (Exception e) {
            logger.error("Failed to send game message in count! {}", e);
        }
    }

    private boolean canPlay(final long memberId) {
        boolean canPlay = false;
        if (data.latestPlayer != memberId) {
            canPlay = true;
        } else {
            if (warningMessage == 0L) {
                logger.trace("Double count performed by player: {}", memberId);
                data.channel.sendMessage("%s You can't count twice in a row!".formatted(Jarvis.getEmojiFormatted("red_exclamation")))
                        .queue(s -> {
                            warningMessage = s.getIdLong();
                            logger.trace("Sending new warning message...");
                            s.delete().queueAfter(10, TimeUnit.SECONDS, x -> {
                                warningMessage = 0L;
                                logger.trace("Warning message deleted, due to time constraint.");
                            });
                        });
            }
        }
        logger.trace("Latest Player: {} | Current Player: {}", data.latestPlayer, memberId);
        return canPlay;
    }

    private long warningMessage = 0L;

    private synchronized void deleteWarningMessage() {
        try {
            logger.trace("Deleting warning message...");
            if (warningMessage != 0L) {
                data.channel.deleteMessageById(warningMessage).queue(x -> {
                            logger.trace("Warning message deleted.");
                            warningMessage = 0L;
                        },
                        f -> logger.trace("Failed to delete warning message. {}", f));
            }
        } catch (Exception e) {
            logger.error("Failed to delete warning message! {}", e);
        }
    }

    private final Map<Long, List<Long>> playerStreaks = new HashMap<>();

    private boolean computeStreak(@NotNull final MessageReceivedEvent event) {
        logger.trace("Computing streak for player: {}", event.getAuthor().getIdLong());
        final List<Long> times = playerStreaks.computeIfAbsent(event.getAuthor().getIdLong(), k -> new ArrayList<>());
        final long compareTime = event.getMessage().getTimeCreated().toEpochSecond();
        times.add(compareTime);
        if (times.size() < 6) return false;
        times.removeFirst();
        return times.stream().allMatch(time -> (time + 60000) > compareTime);
    }

    public static class Leaderboard extends ArrayList<CountPlayer> {

        public Leaderboard(@NotNull final CountGameData data) {
            super(data.getPlayers());
            data.resetPlayers();
            Jarvis.LOGGER.info("[Server:{}] Creating Leaderboard for finished Count Game",data.serverId);
            Collections.sort(this);
            calculateAndAwardExperience(data.currentNumber);
        }

        private void calculateAndAwardExperience(final int currentNumber) {
            int i = 1;
            for (final CountPlayer player : this) {
                player.experienceGained = calculateExperienceEarned(currentNumber, i++, player.counts, player.wrongCount);
                player.getCollectiveMember()
                        .increment(CollectiveMember.MemberData.EXPERIENCE, player.experienceGained);
            }
        }
    }

    private void deleteMessage(@NotNull final Message message) {
        logger.trace("Deleting new message in Count...");
        try {
            message.delete().queueAfter(300, TimeUnit.MILLISECONDS,
                    s -> logger.info("New message in Count deleted."),
                    f -> logger.warn("Failed to delete message in count channel! {}", f));
        } catch (Exception e) {
            logger.warn("Failed to delete message in count channel! {}", e);
        }
    }

    public static long calculateExperienceEarned(final int currentNumber, final int leaderboardRank, final int counts, final boolean wrongCount) {
        long xp = counts * 2L;
        xp = switch (leaderboardRank) {
            case 1 -> xp * 2L;
            case 2 -> (long) (xp * 1.5);
            case 3 -> (long) (xp * 1.25);
            default -> xp;
        };
        final double countPercentage = ((double) counts / currentNumber) * 100;
        if (countPercentage > 75) xp += 25;
        else if (countPercentage > 50) xp += 15;
        else if (countPercentage > 25) xp += 5;

        return wrongCount ? xp - 25 : xp;
    }

    private void reset() {
        save();
        playerStreaks.clear();
        data.reset();
    }
}
