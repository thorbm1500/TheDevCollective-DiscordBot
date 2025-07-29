package dev.prodzeus.jarvis.games.count;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.games.count.game.CountGame;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class CountGameHandler extends ListenerAdapter {

    private static final Map<Long, CountGame> games = new HashMap<>();
    public static final String gameOverText = getGameOverText();
    public static final String newHighscoreText = getNewHighscoreText();
    public static final String trophy = Jarvis.getEmojiFormatted("trophy");
    public static final String streak = Jarvis.getEmojiFormatted("streak");
    public static final String correctCountEmoji = Jarvis.getEmojiFormatted("correct");
    public static final String incorrectCountEmoji = Jarvis.getEmojiFormatted("incorrect");
    public static final String beta = Jarvis.getEmojiFormatted("beta_1") + Jarvis.getEmojiFormatted("beta_2") + Jarvis.getEmojiFormatted("beta_3");

    public CountGameHandler() {
        Jarvis.registerShutdownHook(this::shutdown);
        for (final Guild guild : Jarvis.getGuilds()) {
            newGame(guild);
        }
    }

    @Override
    public void onMessageReceived(@NotNull final MessageReceivedEvent event) {
        if (!isPlayer(event)) return;
        getGame(event.getGuild().getIdLong()).run(event);
    }

    private void newGame(@NotNull final Guild guild) {
        new CountGame(guild.getIdLong());
    }

    public static void addGame(final long id, @NotNull final CountGame game) {
        games.put(id, game);
    }

    public static void removeGame(final long serverId) {
        games.remove(serverId);
    }

    private CountGame getGame(final long serverId) {
        if (games.containsKey(serverId)) return games.get(serverId);
        else return games.put(serverId,new CountGame(serverId));
    }

    private static @NotNull String getGameOverText() {
        return "# Game Over "
               + Jarvis.getEmojiFormatted("lightning")
               + " \n%s: **%d** %s\n-# Ended by: %s *-25xp Penalty* \n## Leaderboard "
               + Jarvis.getEmojiFormatted("rocket")
               + " \n1. **%s** *%+dxp* \n -# **%s Counts | %s**\n2. **%s** *%+dxp* \n -# **%s Counts | %s**\n3. **%s** *%+dxp* \n -# **%s Counts | %s**\n-# ***and %s other players..***";
    }

    private static @NotNull String getNewHighscoreText() {
        return " \n### %s **Just broke the record! New highscore: %d** " + Jarvis.getEmojiFormatted("confetti") + "\n-# _Previous highscore of **%d** was made: <t:%d:R>_";
    }

    @Contract(pure = true)
    public boolean isPlayer(@NotNull final MessageReceivedEvent event) {
        return !(event.isWebhookMessage() || event.getAuthor().isBot() || event.getAuthor().isSystem());
    }

    public static @NotNull String formatPercentage(final int currentNumber, @NotNull final Number counts) {
        return "%"+"%.2f".formatted(((counts.floatValue() / currentNumber) * 100f));
    }

    public void shutdown() {
        games.values().forEach(CountGame::save);
        games.clear();
        Jarvis.jda().removeEventListener(this);
    }
}
