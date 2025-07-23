package dev.prodzeus.jarvis.games;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.configuration.enums.Channel;
import dev.prodzeus.jarvis.enums.Counts;
import dev.prodzeus.jarvis.enums.Emoji;
import dev.prodzeus.jarvis.enums.Member;
import dev.prodzeus.jarvis.enums.ServerCount;
import dev.prodzeus.jarvis.logger.Logger;
import dev.prodzeus.jarvis.logger.Response;
import dev.prodzeus.jarvis.utils.Utils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static dev.prodzeus.jarvis.logger.Logger.log;
import static java.util.logging.Level.*;

public class Count extends ListenerAdapter {

    private static long latestPlayer = 0;
    private static int currentNumber = 0;
    private static long lastWarning = 0;
    private static boolean highscoreAnnounced = false;
    private static int highscore = 0;
    private static long timeOfHighscore = 0;

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
        Logger.log("Count data saved to database!");
    }

    @Override
    public synchronized void onMessageReceived(@NotNull MessageReceivedEvent event) {
        final User user = event.getAuthor();
        if (event.isWebhookMessage() || user.isBot() || user.isSystem()) return;

        final Member member = Utils.getMember(event.getMember());

        final MessageChannel channel = event.getChannel();
        if (channel.getIdLong() != Channel.COUNT.id) return;

        if (event.getMessage().getType().canDelete()) {
            try {
                event.getMessage().delete().queue(null, f -> log(WARNING,"Failed to delete message in count channel for server %s! %s",event.getGuild().getId(),f));
            } catch (Exception e) {
                log(WARNING,"Failed to delete message in count channel for server %s! %s",event.getGuild().getId(),e);
            }
        }

        int countedNumber;
        try {
            System.out.println("RAW: " + event.getMessage().getContentRaw());
            countedNumber = Integer.parseInt(event.getMessage().getContentRaw());
        } catch (Exception ignored) { return; }

        if (latestPlayer == member.id()) {
            if (lastWarning > System.currentTimeMillis()) return;
            else lastWarning = System.currentTimeMillis() + 1500;
            new Response(event)
                    .message("%s You can't count twice in a row!", Emoji.EXCLAMATION.getFormatted())
                    .deleteAfter(15)
                    .send();
            return;
        } else latestPlayer = member.id();

        final Counts counts = Bot.database.getUserCounts(member);

        if (countedNumber == currentNumber++) {
            if (countedNumber > highscore) {
                if (highscoreAnnounced) {
                    channel.sendMessage("%s **%d**\n-# Correct counts: %d  |  Incorrect counts: %d"
                                    .formatted(member.getMention(), countedNumber, counts.correctCounts(), counts.incorrectCounts()))
                            .queue(s -> s.addReaction(Emoji.TROPHY.getEmoji()).queue());
                } else {
                    channel.sendMessage("%s **%d**\n-# Correct counts: %d  |  Incorrect counts: %d\n{Emojis.CELEBRATION} You just broke the record! New highscore: **%d**\n-# Previous highscore of %d was made: <t:%d:R>"
                                    .formatted(member.getMention(), countedNumber, counts.correctCounts(), counts.incorrectCounts(), countedNumber, highscore, timeOfHighscore))
                            .queue(s -> s.addReaction(Emoji.TROPHY.getEmoji()).queue());
                    highscoreAnnounced = true;
                }
                highscore = countedNumber;
                timeOfHighscore = event.getMessage().getTimeCreated().toEpochSecond();
            } else channel.sendMessage("%s **%d**\n-# Correct counts: %d  |  Incorrect counts: %d"
                            .formatted(member.getMention(), countedNumber, counts.correctCounts(), counts.incorrectCounts()))
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
                        .queue(null,f -> log(WARNING,"Failed to update topic of count channel for server %s! %s",member.server(),f));
            } else {
                channel.sendMessage("Congratulations %s. You've ruined the count for everyone else a total of %d times! The next number was indeed *not* **%d** but **%d**."
                                .formatted(member.getMention(), counts.incorrectCounts() + 1, countedNumber, currentNumber)).queue();
            }
        }
    }
}