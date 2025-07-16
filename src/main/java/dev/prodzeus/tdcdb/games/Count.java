package dev.prodzeus.tdcdb.games;

import dev.prodzeus.tdcdb.enums.Emoji;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Count {

    private static String latestPlayer = "";
    private static int currentNumber = 0;

    public static void count(final MessageReceivedEvent event) {
        final MessageChannel channel = event.getChannel();
        if (!channel.getId().equalsIgnoreCase("1379134564340863086")) return;
        try {
            if (event.getMessage().getType().canDelete()) event.getMessage().delete();
        } catch (Exception ignored) {}

        final User user = event.getAuthor();
        if (user.isBot()) return;
        if (latestPlayer.equals(user.getId())) {
            event.getMessage().getChannel().sendMessage("%s You can't count twice in a row, %s!".formatted(Emoji.EXCLAMATION.id,user.getAsMention()));
            return;
        }
        int countedNumber = 0;

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