package dev.prodzeus.tdcdb.games;

import dev.prodzeus.tdcdb.enums.Emoji;
import dev.prodzeus.tdcdb.logger.Response;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Count {

    private static final String countChannel = "1379134564340863086";

    private static String latestPlayer = "";
    private static int currentNumber = 0;
    private static final Map<String,Long> warningCooldowns = new HashMap<>();

    public static void count(final MessageReceivedEvent event) {
        final MessageChannel channel = event.getChannel();
        if (!channel.getId().equalsIgnoreCase(countChannel)) return;
        try {
            if (event.getMessage().getType().canDelete()) event.getMessage().delete().queue();
        } catch (Exception e) {
            e.getStackTrace();
        }

        final User user = event.getAuthor();
        if (user.isBot()) return;
        if (latestPlayer.equals(user.getId())) {
            if (warningCooldowns.getOrDefault(latestPlayer, 0L) > System.currentTimeMillis()) return;
            else warningCooldowns.put(latestPlayer, (System.currentTimeMillis() + 15));
            new Response(event)
                    .message("%s You can't count twice in a row, %s!".formatted(Emoji.EXCLAMATION.id,user.getAsMention()))
                    .deleteAfter(15)
                    .send();
            return;
        }
        int countedNumber;

        try {
            countedNumber = Integer.parseInt(user.getId());
        } catch (Exception ignored) { return; }

        latestPlayer = user.getId();

        if (countedNumber == currentNumber++) {
            channel.sendMessage("");
        }
    }

    public static String getPlayerId() {
        return latestPlayer;
    }
}