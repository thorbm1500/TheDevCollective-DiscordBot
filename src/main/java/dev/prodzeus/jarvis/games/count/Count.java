package dev.prodzeus.jarvis.games.count;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.configuration.enums.Channel;
import dev.prodzeus.jarvis.enums.CollectiveMember;
import dev.prodzeus.jarvis.enums.Counts;
import dev.prodzeus.jarvis.enums.ServerCount;
import dev.prodzeus.jarvis.response.Response;
import dev.prodzeus.jarvis.utils.Utils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static dev.prodzeus.jarvis.bot.Jarvis.LOGGER;

public class Count extends ListenerAdapter {

    private static long latestPlayer = 0L;
    private static int currentNumber;
    private static long lastWarning = 0L;
    private static boolean highscoreAnnounced = false;
    private static int highscore;
    private static long timeOfHighscore;
    private static final Map<Long, List<Long>> playerStreaks = new HashMap<>();

    public Count() {
        LOGGER.debug("New Count Listener created.");
        final ServerCount count = Jarvis.DATABASE.getServerCountStats(Utils.getGuild().getIdLong());
        currentNumber = count.current();
        highscore = count.highscore();
        timeOfHighscore = count.epochTime();
    }

    public static void shutdown() {
        save();
    }

    private static void save() {
        Jarvis.DATABASE.saveServerCountStats(new ServerCount(Utils.getGuild().getIdLong(), currentNumber, highscore, timeOfHighscore));
        Jarvis.LOGGER.info("Count data saved to database!");
    }

    @Override
    public synchronized void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.isWebhookMessage()) return;

        final TextChannel channel = event.getChannel().asTextChannel();
        if (channel.getIdLong() != Channel.COUNT.id) return;

        final User user = event.getAuthor();
        if (user.isBot() || user.isSystem()) return;

        try {
            event.getMessage().delete().queueAfter(275, TimeUnit.MILLISECONDS,
                    null,
                    f -> Jarvis.LOGGER.warn("Failed to delete message in count channel for server {}! {}", event.getGuild().getId(), f));
        } catch (Exception e) {
            Jarvis.LOGGER.warn("Failed to delete message in count channel for server {}! {}", event.getGuild().getId(), e);
        }

        int countedNumber;
        try {
            countedNumber = Integer.parseInt(event.getMessage().getContentRaw());
        } catch (Exception ignored) {
            return;
        }

        final CollectiveMember collectiveMember = Utils.getCollectiveMember(event.getMember());

        if (latestPlayer == collectiveMember.id()) {
            if (lastWarning > System.currentTimeMillis()) return;
            else lastWarning = System.currentTimeMillis() + 15000;
            new Response(event)
                    .message("%s You can't count twice in a row!", Jarvis.BOT.getEmojiFormatted("red_exclamation"))
                    .deleteAfter(15)
                    .send();
            return;
        } else latestPlayer = collectiveMember.id();

        final boolean gameOver = countedNumber != currentNumber;
        if (gameOver && currentNumber == 1) return;
        else currentNumber++;
        Jarvis.DATABASE.saveUserCounts(collectiveMember, gameOver ? 0 : 1, gameOver ? 1 : 0);

        final Counts counts = Jarvis.DATABASE.getUserCounts(collectiveMember);

        String message;
        if (gameOver) {
            CountPlayer.sort();
            final int leaderboardSize = CountPlayer.getCountPlayers().size();
            message = "# Game Over %s".formatted(Jarvis.BOT.getEmojiFormatted("lightning") +
                                                 "\n-# Ended by: %s - %s: **%d** %s\n".formatted(collectiveMember.getMention(), "%s", currentNumber,"%s") +
                                                 "## Leaderboard %s \n".formatted(Jarvis.BOT.getEmojiFormatted("rocket")) +
                                                 "1. %s \n -# **%s Counts | %s**\n".formatted(
                                                         leaderboardSize != 0 ? "<@"+CountPlayer.getCountPlayers().getFirst().id+">" : "N/A",
                                                         leaderboardSize != 0 ? CountPlayer.getCountPlayers().getFirst().counts : "N/A",
                                                         leaderboardSize != 0 ? "%"+((CountPlayer.getCountPlayers().removeFirst().counts / currentNumber) * 100) : "N/A") +
                                                 "2. %s \n -# **%s Counts | %s**\n".formatted(
                                                         leaderboardSize > 1 ? "<@"+CountPlayer.getCountPlayers().getFirst().id+">" : "N/A",
                                                         leaderboardSize > 1 ? CountPlayer.getCountPlayers().getFirst().counts : "N/A",
                                                         leaderboardSize > 1 ? "%"+((CountPlayer.getCountPlayers().removeFirst().counts / currentNumber) * 100) : "N/A") +
                                                 "3. %s \n -# **%s Counts | %s**\n".formatted(
                                                         leaderboardSize > 2 ? "<@"+CountPlayer.getCountPlayers().getFirst().id+">" : "N/A",
                                                         leaderboardSize > 2 ? CountPlayer.getCountPlayers().getFirst().counts : "N/A",
                                                         leaderboardSize > 2 ? "%"+((CountPlayer.getCountPlayers().removeFirst().counts / currentNumber) * 100) : "N/A"));
            if (highscoreAnnounced) {
                highscoreAnnounced = false;
                channel.sendMessage(message.formatted("New Highscore",Jarvis.BOT.getEmojiFormatted("trophy")))
                        .and(channel.getManager().setTopic("Server Highscore: %d".formatted(highscore)))
                        .queue(null, f -> LOGGER.warn("RestAction failed! Attempted to send new highscore message, and update topic to new highscore, but failed. {}", f));
            } else channel.sendMessage(message.formatted("Score","")).queue();
            currentNumber = 1;
            save();
            playerStreaks.clear();
            CountPlayer.clear();
        } else {
            CountPlayer.increment(collectiveMember.id());
            final boolean isOnStreak = computeStreak(event);
            message = "%s **%d**%s\n-#%s Rank: %s | Correct counts: %d  |  Incorrect counts: %d".formatted(collectiveMember.getMention(), countedNumber, "%s", "%s", counts.level(), counts.correctCounts(), counts.incorrectCounts());
            if (countedNumber > highscore) {
                message = message.formatted(" " + Jarvis.BOT.getEmojiFormatted("trophy"), isOnStreak ? " " + Jarvis.BOT.getEmojiFormatted("streak") : "");
                if (!highscoreAnnounced) {
                    highscoreAnnounced = true;
                    message = "### %s **Just broke the record! New highscore: %d** %s\n-# _Previous highscore of **%d** was made: <t:%d:R>_"
                            .formatted(collectiveMember.getMention(), countedNumber, Jarvis.BOT.getEmojiFormatted("confetti"), highscore, timeOfHighscore);
                }
                highscore = countedNumber;
                timeOfHighscore = event.getMessage().getTimeCreated().toEpochSecond();
            } else {
                message = message.formatted(isOnStreak ? " " + Jarvis.BOT.getEmojiFormatted("streak") : "", "");
            }
            channel.sendMessage(message).queue();
        }
    }

    private boolean computeStreak(@NotNull final MessageReceivedEvent event) {
        final List<Long> times = playerStreaks.computeIfAbsent(event.getMember().getIdLong(), k -> new ArrayList<>());
        final long compareTime = event.getMessage().getTimeCreated().toEpochSecond();
        times.add(compareTime);
        if (times.size() < 6) return false;
        times.removeFirst();
        return times.stream().allMatch(time -> (time + 60000) > compareTime);
    }

    private static class CountPlayer implements Comparable<CountPlayer> {

        private static final List<CountPlayer> countPlayers = new ArrayList<>();

        public final long id;
        public int counts = 1;

        public CountPlayer(final long id) {
            this.id = id;
            countPlayers.add(this);
        }

        public static void clear() {
            countPlayers.clear();
        }

        @NotNull
        public static CountPlayer get(final long id) {
            return countPlayers.stream().filter(player -> player.id == id).findFirst().orElseGet(() -> new CountPlayer(id));
        }

        public static void increment(final long id) {
            get(id).counts++;
        }

        public static void sort() {
            countPlayers.sort(CountPlayer::compareTo);
        }

        @Override
        public int compareTo(@NotNull CountPlayer o) {
            return Integer.compare(counts, o.counts);
        }

        public static List<CountPlayer> getCountPlayers() {
            return countPlayers;
        }
    }
}