package dev.prodzeus.jarvis.games.count;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.Marker;
import dev.prodzeus.logger.SLF4JProvider;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CountGameHandler {

    private static final Logger LOGGER = SLF4JProvider.get().getLoggerFactory().getLogger("GameHandler");
    private static final Marker MARKER = SLF4JProvider.get().getMarkerFactory().getMarker("Count");

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

    private void newGame(@NotNull final Guild guild) {
        new CountGame(guild.getIdLong());
    }

    public static void addGame(final long id, @NotNull final CountGame game) {
        try {
            Jarvis.jda().addEventListener(game);
            LOGGER.trace(MARKER,"[Server:{}] Listener registered.", id);
            games.put(id, game);
            LOGGER.debug(MARKER,"[Server:{}] New game registered.", id);
        } catch (Exception e) {
            LOGGER.warn(MARKER,"[Server:{}] Failed to register new game. {}", id, e);
        }
    }

    public static void removeGame(final long id) {
        try {
            Jarvis.jda().removeEventListener(games.remove(id));
        } catch (Exception e) {
            LOGGER.warn(MARKER,"[Server:{}] Failed to unregister game! {}", id, e);
        }
    }

    private CountGame getGame(final long serverId) {
        if (games.containsKey(serverId)) return games.get(serverId);
        else return games.put(serverId,new CountGame(serverId));
    }

    private static @NotNull String getGameOverText() {
        return "# Game Over "
               + Jarvis.getEmojiFormatted("lightning")
               + " \n%s: **%s** %s\n-# Ended by: %s *-25xp penalty* \n## Leaderboard "
               + Jarvis.getEmojiFormatted("rocket")
               + " \n1. **%s** *%+dxp* \n -# **%s Counts | %s**\n2. **%s** *%+dxp* \n -# **%s Counts | %s**\n3. **%s** *%+dxp* \n -# **%s Counts | %s**\n-# ***and %d other players..***";
    }

    private static @NotNull String getNewHighscoreText() {
        return " \n### %s **Just broke the record! New highscore: %d** " + Jarvis.getEmojiFormatted("confetti") + "\n-# _Previous highscore of **%d** was made: <t:%d:R>_";
    }

    public static @NotNull String formatPercentage(final int currentNumber, @NotNull final Number counts) {
        return "%"+"%.2f".formatted(((counts.floatValue() / currentNumber) * 100f));
    }

    public void shutdown() {
        LOGGER.info(MARKER,"Executing shutdown procedure...");
        if (games.isEmpty()) return;
        final Iterator<CountGame> iterator = games.values().iterator();
        while (iterator.hasNext()) {
            try {
                final CountGame game = iterator.next();
                if (game != null) game.shutdown();
                iterator.remove();
            } catch (Exception e) {
                LOGGER.error(MARKER,"Exception thrown during shutdown procedure! {}", e);
            }
        }
    }
}
