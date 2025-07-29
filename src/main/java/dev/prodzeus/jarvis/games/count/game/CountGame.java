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

public final class CountGame {

    private final boolean enabled;
    private final long serverId;
    private final long channelId;
    private final TextChannel channel;
    private long sync = 0L;

    private long latestPlayer = 0L;
    private int currentNumber;
    private final Map<Long, CountPlayer> counts = new HashMap<>();

    private boolean highscoreAnnounced = false;
    private int highscore;
    private long timeOfHighscore;

    private final List<CountPlayer> countPlayers = new ArrayList<>();

    public CountGame(final long serverId) {
        LOGGER.debug("New Count Game instance created for server {}!", serverId);
        this.serverId = serverId;
        this.channelId = Channels.get(serverId).countChannel;
        if (this.channelId == 0) {
            this.channel = null;
            LOGGER.error("Could not find Count channel for server {}! Is the channel's ID registered?", serverId);
        } else this.channel = Jarvis.jda().getTextChannelById(channelId);

        if (channel != null) {
            this.enabled = true;
            CountGameHandler.addGame(serverId, this);
        } else {
            this.enabled = false;
            return;
        }

        final ServerCount count = Jarvis.DATABASE.getServerCountStats(serverId);
        this.currentNumber = count.current();
        this.highscore = count.highscore();
        this.timeOfHighscore = count.epochTime();
        sync = Jarvis.DATABASE.getSyncMessage(serverId);
        if (sync != 0) {
            final long oldSyncId = sync;
            channel.deleteMessageById(sync).queue(null,
                    f -> LOGGER.warn("Failed to delete sync message with ID {} in count! {}", oldSyncId, f));
            sync = 0;
        }
        channel.sendMessage("## %s Game Sync\nNext number: **%d**"
                        .formatted(Jarvis.getEmojiFormatted("sync"), currentNumber))
                .queue(s -> {
                    this.sync = s.getIdLong();
                    Jarvis.DATABASE.saveSyncMessage(serverId, this.sync);
                });
    }

    public void save() {
        Jarvis.DATABASE.saveServerCountStats(new ServerCount(serverId, currentNumber, highscore, timeOfHighscore));
        LOGGER.info("Count data saved to database for server {}!", serverId);
    }

    public void run(@NotNull final MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != channelId) return;
        else if (!enabled) {
            LOGGER.warn("Count Game is disabled for server {}, due to errors. Ignoring message event.", serverId);
            return;
        }

        final Message message = event.getMessage();

        deleteMessage(message);

        int countedNumber;
        try {
            countedNumber = Integer.parseInt(message.getContentRaw(), 10);
        } catch (Exception e) {
            if (!(e instanceof NumberFormatException)) LOGGER.warn("Failed to parse message to Integer in count! {}", e);
            return;
        }

        final CollectiveMember collectiveMember = MemberManager.getCollectiveMember(event.getAuthor().getIdLong(),serverId);

        if (!canPlay(collectiveMember)) return;

        if (sync != 0) {
            channel.deleteMessageById(sync).queue(
                    s -> Jarvis.DATABASE.clearSyncMessage(serverId),
                    f -> LOGGER.warn("Failed to delete sync message with ID {} in count! {}", sync, f));
            sync = 0;
        }

        String text;
        if (countedNumber == currentNumber++) {
            deleteWarningMessage(channel);
            collectiveMember.incrementCorrectCounts();
            counts.computeIfAbsent(collectiveMember.id, CountPlayer::new).counts++;
            final String streak = computeStreak(event) ? " " + CountGameHandler.streak : "";
            text = "### " + collectiveMember.mention + " **"
                   + countedNumber + "** "
                   + (countedNumber > highscore ? CountGameHandler.trophy : streak) + "\n-# "
                   + (streak.isEmpty() ? collectiveMember.getCountLevelIcon() : (countedNumber > highscore ? streak : "")) + " •  "
                   + CountGameHandler.correctCountEmoji + " **"
                   + collectiveMember.getCorrectCounts() + "**  "
                   + CountGameHandler.incorrectCountEmoji + " **"
                   + collectiveMember.getIncorrectCounts() + "**  •  "
                   + CountGameHandler.beta;
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
            counts.computeIfAbsent(collectiveMember.id, CountPlayer::new).wrongCount = true;
            //if (currentNumber == 1) return;
            collectiveMember.incrementIncorrectCounts();
            try {
                Collections.sort(countPlayers);
            } catch (Exception e) {
                LOGGER.warn("Attempted to sort list of count players but failed! {}", e);
                return;
            }
            final String scoreText = highscoreAnnounced ? "New Highscore" : "Score";
            final String scoreEmoji = highscoreAnnounced ? CountGameHandler.trophy : "";
            final Leaderboard leaderboard = new Leaderboard(currentNumber, counts);
            final CountPlayer firstPlace = leaderboard.removeFirst();
            try {
                if (firstPlace != null) {
                    final CountPlayer secondPlace = leaderboard.removeFirst();
                    if (secondPlace != null) {
                        final CountPlayer thirdPlace = leaderboard.removeFirst();
                        if (thirdPlace != null) {
                            text = CountGameHandler.gameOverText.formatted(scoreText, currentNumber, scoreEmoji, collectiveMember.mention,
                                    "<@" + firstPlace.id + ">", firstPlace.counts, firstPlace.experience, formatPercentage(currentNumber, firstPlace.counts),
                                    "<@" + secondPlace.id + ">", secondPlace.counts, secondPlace.experience, formatPercentage(currentNumber, secondPlace.counts),
                                    "<@" + thirdPlace.id + ">", thirdPlace.counts, thirdPlace.experience, formatPercentage(currentNumber, thirdPlace.counts), countPlayers.size());
                        } else {
                            text = CountGameHandler.gameOverText.formatted(scoreText, currentNumber, scoreEmoji, collectiveMember.mention,
                                    "<@" + firstPlace.id + ">", firstPlace.counts, firstPlace.experience, formatPercentage(currentNumber, firstPlace.counts),
                                    "<@" + secondPlace.id + ">", secondPlace.counts, secondPlace.experience, formatPercentage(currentNumber, secondPlace.counts),
                                    "N/A", "N/A", "N/A", 0);
                        }
                    } else {
                        text = CountGameHandler.gameOverText.formatted(scoreText, currentNumber, scoreEmoji, collectiveMember.mention,
                                "<@" + firstPlace.id + ">", firstPlace.counts, firstPlace.experience, formatPercentage(currentNumber, firstPlace.counts),
                                "N/A", "N/A", "N/A",
                                "N/A", "N/A", "N/A", 0);
                    }
                } else text = CountGameHandler.gameOverText.formatted(scoreText, currentNumber, scoreEmoji, collectiveMember.mention,
                        "N/A", "N/A", "N/A",
                        "N/A", "N/A", "N/A",
                        "N/A", "N/A", "N/A", 0);
            } catch (Exception e) {
                LOGGER.error("Attempted to format GameOver string for count but failed! {}", e);
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
        if (this.latestPlayer == member.id) {
            if (warningMessage == 0L) {
                channel.sendMessage("%s You can't count twice in a row!".formatted(Jarvis.getEmojiFormatted("red_exclamation")))
                        .queue(s -> {
                            warningMessage = s.getIdLong();
                            s.delete().queueAfter(10, TimeUnit.SECONDS, x -> warningMessage = 0L);
                        });
            }
            return false;
        } else {
            this.latestPlayer = member.id;
            return true;
        }
    }

    private long warningMessage = 0L;

    private synchronized void deleteWarningMessage(@NotNull final TextChannel channel) {
        if (warningMessage != 0L) {
            channel.deleteMessageById(warningMessage).queue(x -> warningMessage = 0L);
        }
    }

    private final Map<Long, List<Long>> playerStreaks = new HashMap<>();

    private boolean computeStreak(@NotNull final MessageReceivedEvent event) {
        final List<Long> times = playerStreaks.computeIfAbsent(event.getAuthor().getIdLong(), k -> new ArrayList<>());
        final long compareTime = event.getMessage().getTimeCreated().toEpochSecond();
        times.add(compareTime);
        if (times.size() < 6) return false;
        times.removeFirst();
        return times.stream().allMatch(time -> (time + 60000) > compareTime);
    }

    private static class Leaderboard extends ArrayList<CountPlayer> {

        public Leaderboard(final int currentCount, @NotNull final Map<Long, CountPlayer> players) {
            super(players.values());
            Collections.sort(this);
            calculateAndAwardExperience(currentCount);
        }

        private void calculateAndAwardExperience(final int currentCount) {
            final Map<Long, Long> experience = new HashMap<>();
            int index = 1;
            for (final CountPlayer player : this) {
                final long xp = calculateExperienceEarned(currentCount, index++, player.counts, player.wrongCount);
                player.experience = xp;
                experience.put(player.id, xp);
            }
            MemberManager.addExperience(experience);
        }
    }

    private void deleteMessage(@NotNull final Message message) {
        try {
            message.delete().queueAfter(300, TimeUnit.MILLISECONDS,
                    null,
                    f -> LOGGER.warn("Failed to delete message in count channel! {}", f));
        } catch (Exception e) {
            LOGGER.warn("Failed to delete message in count channel! {}", e);
        }
    }

    public static long calculateExperienceEarned(final int currentCount, final int leaderboardRank, final int counts, final boolean wrongCount) {
        long xp = counts * 2L;
        xp = switch (leaderboardRank) {
            case 1 -> xp * 2L;
            case 2 -> (long) (xp * 1.5);
            case 3 -> (long) (xp * 1.25);
            default -> xp;
        };
        final double countPercentage = ((double) counts / currentCount) * 100;
        if (countPercentage > 75) xp += 25;
        else if (countPercentage > 50) xp += 15;
        else if (countPercentage > 25) xp += 5;

        return wrongCount ? xp - 25 : xp;
    }

    private void reset() {
        currentNumber = 1;
        save();
        playerStreaks.clear();
        countPlayers.clear();
    }
}
