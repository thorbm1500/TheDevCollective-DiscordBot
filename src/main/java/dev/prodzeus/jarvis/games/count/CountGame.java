package dev.prodzeus.jarvis.games.count;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.member.CollectiveMember;
import dev.prodzeus.jarvis.member.MemberManager;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static dev.prodzeus.jarvis.bot.Jarvis.LOGGER;
import static dev.prodzeus.jarvis.bot.Jarvis.getEmojiFormatted;
import static dev.prodzeus.jarvis.games.count.CountGameHandler.formatPercentage;
import static dev.prodzeus.jarvis.utility.Util.isValidMessageEvent;

public class CountGame extends ListenerAdapter {

    private final Logger logger;

    private final CountGameData data;
    private static final String correctCountString = "### %s **%d** %s\n-# %s •  %s **%d**  %s **%d**  •  %s";

    public CountGame(final long serverId) {
        logger = SLF4JProvider.get().getLogger("Count:" + serverId);
        this.data = Jarvis.DATABASE.getCountGameData(serverId);
        if (data == null) logger.debug("Aborting creation of new game instance...");
        else CountGameHandler.addGame(serverId, this);
    }

    public void save() {
        data.save();
    }

    public void shutdown() {
        save();
    }

    @Override
    public void onMessageReceived(@NotNull final MessageReceivedEvent event) {
        try {
            if (event.getChannel().getIdLong() != data.channelId || !isValidMessageEvent(event)) return;

            final Integer countedNumber = getCountedNumber(event.getMessage(), event.getAuthor().getIdLong());
            if (countedNumber == null) return;

            String text;
            if (data.handleCount(event, countedNumber)) {
                deleteWarningMessage();
                final String streak = computeStreak(event) ? " " + CountGameHandler.streak : "";
                final CollectiveMember collectiveMember = MemberManager.getCollectiveMember(data.serverId, event.getAuthor().getIdLong());
                text = correctCountString.formatted(collectiveMember.mention, countedNumber,
                        data.highscoreAnnounced ? CountGameHandler.trophy : streak,
                        streak.isEmpty() ? collectiveMember.getCountLevelIcon() : (data.highscoreAnnounced ? streak : ""),
                        CountGameHandler.correctCountEmoji, collectiveMember.getData(CollectiveMember.MemberData.CORRECT_COUNTS),
                        CountGameHandler.incorrectCountEmoji, collectiveMember.getData(CollectiveMember.MemberData.INCORRECT_COUNTS),
                        CountGameHandler.beta);
            } else {
                //if (currentNumber == 1) return;
                final FinishedGameStats gameStats = data.getGameStats();
                prepareNextGame();
                StringBuilder builder = new StringBuilder();
                builder.append("# Game Over ")
                        .append(getEmojiFormatted("lightning")).append(" \n")
                        .append((data.highscoreAnnounced ? "New Highscore" : "Score"))
                        .append(": **").append(gameStats.count).append("** ")
                        .append((data.highscoreAnnounced ? CountGameHandler.trophy : ""))
                        .append("\n-# Ended by: <@").append(data.latestPlayer).append("> | __-25xp penalty__")
                        .append("\n## Leaderboard").append(Jarvis.getEmojiFormatted("rocket"));
                CountPlayer player;
                for (int i = 1; i < 4; i++) {
                    builder.append("\n").append(i).append(". **");
                    player = gameStats.getNext();
                    if (player == null) builder.append("N/A** *0xp* \n -# **0 Counts | 0%**");
                    else {
                        builder.append("<@").append(player.id).append(">** *")
                                .append(player.experienceGained)
                                .append("xp* \n -# **").append(player.counts).append(" Counts | ")
                                .append(formatPercentage(gameStats.count, player.counts))
                                .append("%**");
                    }
                }
                text = builder.toString();
            }
            try {
                data.channel.sendMessage(text)
                        .queue(s -> logger.trace("New message sent in count."),
                                f -> logger.error("Failed to send game message in count! {}", f));
            } catch (Exception e) {
                logger.error("Failed to send game message in count! {}", e);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to handle MessageReceivedEvent! {}", e);
        }
    }

    private Integer getCountedNumber(@NotNull final Message message, final long memberId) {
        deleteMessage(message);

        int countedNumber;
        try {
            logger.trace("Checking for integer...");
            countedNumber = Integer.parseInt(message.getContentRaw(), 10);
        } catch (Exception e) {
            if (!(e instanceof NumberFormatException)) logger.warn("Failed to parse message to Integer! {}", e);
            return null;
        }
        logger.trace("Integer {} found.", countedNumber);

        return canPlay(memberId) ? countedNumber : null;
    }

    private boolean canPlay(final long id) {
        if (data.latestPlayer != id) return true;
        else {
            if (warningMessage == 0L) {
                logger.trace("Double count performed by player: {}", id);
                data.channel.sendMessage("%s You can't count twice in a row!".formatted(getEmojiFormatted("red_exclamation")))
                        .queue(s -> {
                            warningMessage = s.getIdLong();
                            logger.trace("Sending new warning message...");
                            s.delete().queueAfter(10, TimeUnit.SECONDS, x -> {
                                warningMessage = 0L;
                                logger.trace("Warning message deleted, due to time constraint.");
                            });
                        });
            }
            return false;
        }
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
        final long createdAt = event.getMessage().getTimeCreated().toEpochSecond();
        times.add(createdAt);
        final long compareTime = createdAt + 20000;
        if (times.size() < 6) return false;
        times.removeFirst();
        return times.stream().allMatch(time -> time > compareTime);
    }

    private void deleteMessage(@NotNull final Message message) {
        logger.trace("Deleting new message in Count...");
        try {
            message.delete().queueAfter(300, TimeUnit.MILLISECONDS,
                    s -> logger.trace("New message in Count deleted."),
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
        data.reset();
        playerStreaks.clear();
    }

    private void prepareNextGame() {
        reset();
        save();
    }

    public static class FinishedGameStats extends ArrayList<CountPlayer> {

        final int count;

        public FinishedGameStats(@NotNull final CountGameData data) {
            super(data.getPlayers());
            Jarvis.LOGGER.info("[Server:{}] Creating Leaderboard...", data.serverId);
            this.count = data.currentNumber;
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

        public CountPlayer getNext() {
            return isEmpty() ? null : removeFirst();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof FinishedGameStats stats && count == stats.count && this.equals(stats);
        }

        @Override
        public int hashCode() {
            return Objects.hash(count, super.hashCode());
        }
    }
}
