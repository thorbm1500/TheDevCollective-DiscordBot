package dev.prodzeus.jarvis.games.count;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.games.count.game.CountGame;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class CountGameHandler extends ListenerAdapter {

    private static final Map<Long, CountGame> games = new HashMap<>();
    public static final String gameOverText = getGameOverText();
    public static final String newHighscoreText = getNewHighscoreText();
    public static final String trophy = Jarvis.BOT.getEmojiFormatted("trophy");
    public static final String streak = Jarvis.BOT.getEmojiFormatted("streak");
    public static final String correctCountEmoji = Jarvis.BOT.getEmojiFormatted("correct");
    public static final String incorrectCountEmoji = Jarvis.BOT.getEmojiFormatted("incorrect");

    public CountGameHandler() {
        Jarvis.jda().getGuilds().forEach(this::newGame);
        Jarvis.registerShutdownHook(this::shutdown);
    }

    @Override
    public void onMessageReceived(@NotNull final MessageReceivedEvent event) {
        if (!isPlayer(event)) return;
        getGame(event.getGuild().getIdLong()).run(event);
    }

    private void newGame(@NotNull final Guild guild) {
        games.put(guild.getIdLong(), new CountGame(guild.getIdLong()));
    }

    public static void removeGame(final long serverId) {
        games.remove(serverId);
    }

    private CountGame getGame(final long serverId) {
        return games.computeIfAbsent(serverId, CountGame::new);
    }

    private static String getGameOverText() {
        return "# Game Over "
               + Jarvis.BOT.getEmojiFormatted("lightning")
               + " \n%s: **%d** %s\n-# Ended by: %s \n## Leaderboard "
               + Jarvis.BOT.getEmojiFormatted("rocket")
               + " \n1. **%s** \n -# **%s Counts | %s**\n2. **%s** \n -# **%s Counts | %s**\n3. **%s** \n -# **%s Counts | %s**\n-# ***and %s other players..***";
    }

    private static String getNewHighscoreText() {
        return " \n### %s **Just broke the record! New highscore: %d** " + Jarvis.BOT.getEmojiFormatted("confetti") + "\n-# _Previous highscore of **%d** was made: <t:%d:R>_";
    }

    public boolean isPlayer(@NotNull final MessageReceivedEvent event) {
        return !(event.isWebhookMessage() || event.getAuthor().isBot() || event.getAuthor().isSystem());
    }

    public static String formatPercentage(final int currentNumber,final Number counts) {
        return "%"+String.valueOf(((counts.floatValue() / currentNumber) * 100f)).substring(0,5);
    }

    public void shutdown() {
        games.values().forEach(CountGame::save);
        games.clear();
        Jarvis.jda().removeEventListener(this);
    }
}
