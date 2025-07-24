package dev.prodzeus.jarvis.games.count;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.configuration.enums.Channel;
import dev.prodzeus.jarvis.enums.Counts;
import dev.prodzeus.jarvis.enums.Emoji;
import dev.prodzeus.jarvis.enums.Member;
import dev.prodzeus.jarvis.enums.ServerCount;
import dev.prodzeus.jarvis.response.Response;
import dev.prodzeus.jarvis.utils.Utils;
import dev.prodzeus.logger.Logger;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class Count extends ListenerAdapter {

    private static final Logger logger = Bot.INSTANCE.logger;

    private static long latestPlayer = 0L;
    private static int currentNumber = 0;
    private static long lastWarning = 0L;
    private static boolean highscoreAnnounced = false;
    private static int highscore = 0;
    private static long timeOfHighscore = 0L;

    public Count() {
        final ServerCount count = Bot.database.getServerCountStats(Utils.getGuild().getIdLong());
        currentNumber = count.current();
        highscore = count.highscore();
        timeOfHighscore = count.epochTime();
    }

    public static void shutdown() {
        save();
    }

    private static void save() {
        Bot.database.saveServerCountStats(new ServerCount(Utils.getGuild().getIdLong(), currentNumber, highscore, timeOfHighscore));
        logger.info("Count data saved to database!");
    }

    @Override
    public synchronized void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.isWebhookMessage()) return;

        final User user = event.getAuthor();
        if (user.isBot() || user.isSystem()) return;

        final MessageChannel channel = event.getChannel();
        if (channel.getIdLong() != Channel.COUNT.id) return;

        if (event.getMessage().getType().canDelete()) {
            try {
                event.getMessage().delete().queueAfter(500, TimeUnit.MILLISECONDS,
                        null,
                        f -> logger.warn("Failed to delete message in count channel for server {}! {}",event.getGuild().getId(),f));
            } catch (Exception e) {
                logger.warn("Failed to delete message in count channel for server {}! {}",event.getGuild().getId(),e);
            }
        }

        int countedNumber;
        try {
            countedNumber = Integer.parseInt(event.getMessage().getContentRaw());
        } catch (Exception ignored) { return; }

        final Member member = Utils.getMember(event.getMember());

        if (latestPlayer == member.id()) {
            if (lastWarning > System.currentTimeMillis()) return;
            else lastWarning = System.currentTimeMillis() + 15000;
            new Response(event)
                    .message("%s You can't count twice in a row!", Emoji.EXCLAMATION.getString())
                    .deleteAfter(15)
                    .send();
            return;
        } else latestPlayer = member.id();

        final Counts counts = Bot.database.getUserCounts(member);

        if (countedNumber == currentNumber++) {
            if (countedNumber > highscore) {
                if (highscoreAnnounced) {
                    channel.sendMessage("%s **%d**\n-# Rank: %s | Correct counts: %d  |  Incorrect counts: %d"
                                    .formatted(member.getMention(), countedNumber, CountLevel.getCountLevel(counts.correctCounts()).emoji.getString(), counts.correctCounts(), counts.incorrectCounts()))
                            .queue(s -> s.addReaction(Emoji.TROPHY.getEmoji()).queue());
                } else {
                    channel.sendMessage("%s **%d**\n-# Rank: %s | Correct counts: %d  |  Incorrect counts: %d\n{Emojis.CELEBRATION} You just broke the record! New highscore: **%d**\n-# Previous highscore of %d was made: <t:%d:R>"
                                    .formatted(member.getMention(), countedNumber, CountLevel.getCountLevel(counts.correctCounts()).emoji.getString(), counts.correctCounts(), counts.incorrectCounts(), countedNumber, highscore, timeOfHighscore))
                            .queue(s -> s.addReaction(Emoji.TROPHY.getEmoji()).queue());
                    highscoreAnnounced = true;
                }
                highscore = countedNumber;
                timeOfHighscore = event.getMessage().getTimeCreated().toEpochSecond();
            } else channel.sendMessage("%s **%d**\n-# Rank: %s | Correct counts: %d  |  Incorrect counts: %d"
                            .formatted(member.getMention(), countedNumber, CountLevel.getCountLevel(counts.correctCounts()).emoji.getString(), counts.correctCounts(), counts.incorrectCounts()))
                    .queue();
        } else {
            currentNumber = 1;
            save();
            if (highscoreAnnounced) {
                highscoreAnnounced = false;

                channel.sendMessage("This is why we can't have nice things, %s. You've ruined the count for everyone else a total of %d times! The next number was **%d** and not **%d**.."
                                .formatted(member.getMention(), counts.incorrectCounts() + 1, currentNumber, countedNumber)).and(channel.sendMessage("Anyway, here's the new highscore, I guess.. **%d** %s"
                        .formatted(highscore,Emoji.TROPHY.formatted))).queue();

                Utils.getTextChannel(channel.getIdLong()).getManager().setTopic("Server Highscore: %d".formatted(highscore))
                        .queue(null,f -> logger.warn("Failed to update topic of count channel for server {}! {}",member.server(),f));
            } else {
                channel.sendMessage("Congratulations %s. You've ruined the count for everyone else a total of %d times! The next number was indeed *not* **%d** but **%d**."
                                .formatted(member.getMention(), counts.incorrectCounts() + 1, countedNumber, currentNumber)).queue();
            }
        }
    }
}