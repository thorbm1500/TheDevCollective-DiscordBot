package dev.prodzeus.tdcdb.games;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Count {

    private static String latestPlayer = "ty";
    private static int currentNumber = 0;

    public static void count(final MessageReceivedEvent event) {
        final User user = event.getAuthor();
        if (user.isBot()) return;
        if (latestPlayer.equals(user.getId())) {
            event.getMessage().getChannel().sendMessage("{Emojis.EXCLAMATION_MARK} You can't count twice in a row, {author.mention}!");
            return;
        }
    }

    public static String getPlayerId() {
        return latestPlayer;
    }
}