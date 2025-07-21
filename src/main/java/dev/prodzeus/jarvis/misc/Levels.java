package dev.prodzeus.jarvis.misc;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.enums.Emoji;
import dev.prodzeus.jarvis.utils.Utils;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Levels extends ListenerAdapter {
    private Pattern emoji = Pattern.compile(":[^\\s:][^:]*?:");
    private PreparedStatement getMember = Utils.prepareStatement("SELECT (experience, level, last_message) FROM members WHERE id = ?;");
    private PreparedStatement updateMember = Utils.prepareStatement("UPDATE members SET experience=?,level=?,last_message=now() WHERE id=?;");
    private int[] levels = new int[100];

    public Levels() {
        var xp = 0;
        for (var i = 0; i < 100; i++) {
            xp += (int) (5 * (Math.pow(i, 1.75) + 15));
            levels[i] = xp;
        }
    }

    private int getLevelFromXp(int xp) {
        if (xp < levels[0]) {
            return 0;
        }

        int index = Arrays.binarySearch(levels, xp);
        if (xp == levels[index]) {
            return index;
        } else {
            return index - 1;
        }
    }

    @SneakyThrows
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        var xp = 1;

        var content = e.getMessage().getContentRaw();
        xp += Math.min(content.length(), 200) / 50;

        if (!e.getMessage().getAttachments().isEmpty()) xp += 1;

        var emojis = (int) emoji.matcher(content).results().count();
        emojis -= (emojis + 1) % 2;
        xp += Math.min(emojis, 5) - 1;

        var connection = Utils.getConnection();
        try {
            connection.setAutoCommit(false);
            getMember.setLong(1, e.getMember().getIdLong());
            var userData = getMember.executeQuery();
            userData.next();
            var userXp = userData.getInt("experience");
            var userLevel = userData.getInt("level");
            var lastMessage = userData.getTimestamp("last_message");
            if (e.getMessage().getTimeCreated().toEpochSecond() - (lastMessage.getTime() / 1000) > 30) {
                var newXp = userXp + xp;
                var newLevel = getLevelFromXp(xp);
                updateMember.setInt(1, newXp);
                updateMember.setInt(2, newLevel);
                updateMember.executeUpdate();
                if (userLevel < newLevel) {
                    Bot.INSTANCE.executor.execute(() -> {
                        Utils.getGuild().getTextChannelById(Bot.settings.levelChannel).sendMessage("%s %s  is now level: **%d**\n-# Current experience: %d".formatted(Emoji.CONFETTI.id, e.getMember().getAsMention(), newLevel, newXp)).queue();
                        if (newLevel == 1) {
                            e.getGuild().addRoleToMember(UserSnowflake.fromId(e.getMember().getId()), e.getGuild().getRoleById(Bot.settings.levels.get(0)));
                            return;
                        }
                        if (newLevel % 5 != 0) return;
                        e.getGuild().removeRoleFromMember(UserSnowflake.fromId(e.getMember().getId()), e.getGuild().getRoleById(Bot.settings.levels.get((newLevel - 5) / 5)));
                        e.getGuild().addRoleToMember(UserSnowflake.fromId(e.getMember().getId()), e.getGuild().getRoleById(Bot.settings.levels.get(newLevel / 5)));
                    });
                }
            }
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw new RuntimeException(ex);
        } finally {
            connection.setAutoCommit(true);
        }
    }
}
