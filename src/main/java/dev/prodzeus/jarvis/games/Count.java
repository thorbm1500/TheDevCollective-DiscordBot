package dev.prodzeus.jarvis.games;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.configuration.enums.Channels;
import dev.prodzeus.jarvis.enums.Emoji;
import dev.prodzeus.jarvis.logger.Logger;
import dev.prodzeus.jarvis.logger.Response;
import dev.prodzeus.jarvis.utils.Utils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Count extends ListenerAdapter {

    private static String latestPlayer = "";
    private static int currentNumber = 0;
    private static final Map<String, Long> warningCooldowns = new HashMap<>();
    private static boolean highscoreAnnounced = false;
    private static int highscore = 0;
    private static long timeOfHighscore = 0;

    public Count() {
        //todo: Add yaml data saving and reading
        //highscore = ?
    }

    @Override
    public synchronized void onMessageReceived(@NotNull MessageReceivedEvent event) {
        final User user = event.getAuthor();
        if (event.isWebhookMessage() || user.isBot() || user.isSystem()) return;

        final MessageChannel channel = event.getChannel();
        if (!channel.getId().equals(Channels.COUNT.id)) return;

        if (event.getMessage().getType().canDelete()) {
            try {
                event.getMessage().delete().queue();
            } catch (Exception e) {
                Logger.warn("Failed to delete message in count! %s", e.getMessage());
            }
        }

        int countedNumber;
        try {
            countedNumber = Integer.parseInt(event.getMessage().getContentRaw());
        } catch (Exception ignored) {
            return;
        }

        if (latestPlayer.equals(user.getId())) {
            if (warningCooldowns.getOrDefault(latestPlayer, 0L) > System.currentTimeMillis()) return;
            else warningCooldowns.put(latestPlayer, (System.currentTimeMillis() + 15));
            new Response(event)
                    .message("%s You can't count twice in a row, %s!", Emoji.EXCLAMATION.getEmoji().getFormatted(), user.getAsMention())
                    .deleteAfter(15)
                    .send();
            return;
        } else latestPlayer = user.getId();

        final Pair<Integer, Integer> userCounts = Bot.database.getUserCounts((int) user.getIdLong());

        if (countedNumber == currentNumber++) {
            if (countedNumber > highscore) {
                if (highscoreAnnounced) {
                    channel.sendMessage("%s **%d**\\n-# Correct counts: %d  |  Incorrect counts: %d"
                                    .formatted(user.getAsMention(), countedNumber, userCounts.getLeft(), userCounts.getRight()))
                            .queue(s -> s.addReaction(Emoji.TROPHY.getEmoji()).queue());
                } else {
                    channel.sendMessage("%s **%d**\n-# Correct counts: %d  |  Incorrect counts: %d\n{Emojis.CELEBRATION} You just broke the record! New highscore: **%d**\n-# Previous highscore of %d was made: <t:%d:R>"
                                    .formatted(user.getAsMention(), countedNumber, userCounts.getLeft(), userCounts.getRight(), countedNumber, highscore, timeOfHighscore))
                            .queue();
                    highscoreAnnounced = true;
                }
                highscore = countedNumber;
                timeOfHighscore = event.getMessage().getTimeCreated().toEpochSecond();
            } else channel.sendMessage("%s **%d**\\n-# Correct counts: %d  |  Incorrect counts: %d"
                            .formatted(user.getAsMention(), countedNumber, userCounts.getLeft(), userCounts.getRight()))
                    .queue();
        } else {
            if (highscoreAnnounced) {
                //todo: saveHighscore();
                channel.sendMessage("This is why we can't have nice things, %s. You've ruined the count for everyone else a total of %d times! The next number was **%d** and not **%d**.."
                                .formatted(user.getAsMention(), userCounts.getRight() + 1, currentNumber, countedNumber))
                        .queue();
                channel.sendMessage("Anyway, here's the new highscore, I guess.. **%d**"
                                .formatted(highscore))
                        .queue();
                currentNumber = 1;
                highscoreAnnounced = false;
                Utils.getGuild().getTextChannelById(channel.getId()).getManager().setTopic("Server Highscore: %d".formatted(highscore)).queue();
            } else {
                channel.sendMessage("Congratulations %s. You've ruined the count for everyone else a total of %d times! The next number was indeed *not* **%d** but **%d**."
                                .formatted(user.getAsMention(), userCounts.getRight() + 1, countedNumber, currentNumber))
                        .queue();
                currentNumber = 1;
            }
        }
    }
}