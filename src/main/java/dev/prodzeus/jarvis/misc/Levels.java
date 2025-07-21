package dev.prodzeus.jarvis.misc;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.configuration.enums.Channels;
import dev.prodzeus.jarvis.configuration.enums.LevelRoles;
import dev.prodzeus.jarvis.enums.Emoji;
import dev.prodzeus.jarvis.logger.Logger;
import dev.prodzeus.jarvis.utils.Utils;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Levels extends ListenerAdapter {
    private static final Pattern emoji = Pattern.compile(":[^\\s:][^:]*?:");
    private static final int[] levels = new int[100];
    private static final HashMap<Integer, Long> experienceCooldown = new HashMap<>();

    public Levels() {
        int xp = 0;
        for (int i = 0; i < 100; i++) {
            xp += 5 * ((int) Math.pow(i, 1.75) + 15);
            levels[i] = xp;
        }
    }

    private int getLevelFromXp(int xp) {
        if (xp < levels[0]) return 0;
        int index = Arrays.binarySearch(levels, xp);
        if (xp == levels[index]) return index;
        else return index - 1;
    }

    @SneakyThrows
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        final int memberId = (int) e.getMember().getIdLong();
        if (((experienceCooldown.getOrDefault(memberId, 10000000000L) / 1000) - e.getMessage().getTimeCreated().toEpochSecond()) < 30
                || e.getAuthor().isBot() || e.getAuthor().isSystem() || e.isWebhookMessage()) return;
        else experienceCooldown.put(memberId, e.getMessage().getTimeCreated().toEpochSecond());

        int xp = 1;

        String content = e.getMessage().getContentRaw();
        xp += Math.min(content.length(), 200) / 50;

        if (!e.getMessage().getAttachments().isEmpty()) xp += 1;

        long emojis = emoji.matcher(content).results().count();
        emojis -= (emojis + 1) % 2;
        xp += (int) Math.min(emojis, 5) - 1;

        final int currentExperience = Bot.database.getExperience(memberId);
        final int currentLevel = getLevelFromXp(currentExperience);
        final int newExperience = currentExperience + xp;
        final int newLevel = getLevelFromXp(xp);
        final MessageChannel channel = Utils.getGuild().getTextChannelById(Channels.LEVEL.id);
        if (currentLevel < newLevel) {
            channel.sendMessage("%s %s  is now level: **%d**\n-# Current experience: %d"
                    .formatted(Emoji.CONFETTI.id, e.getMember().getAsMention(), newLevel, newExperience)).queue();

            if (newLevel == 1) {
                e.getGuild().addRoleToMember(UserSnowflake.fromId(e.getMember().getId()), e.getGuild().getRoleById(LevelRoles.LEVEL_1.id))
                        .queue(null, f -> Logger.warn("Failed to add role for Level 1 to member %s! %s",String.valueOf(memberId),f.getMessage()));
                return;
            }

            if (newLevel % 5 != 0) return;
            e.getGuild().removeRoleFromMember(UserSnowflake.fromId(e.getMember().getId()), e.getGuild().getRoleById(LevelRoles.getLevelId(newLevel - 5)))
                    .queue(null, f -> Logger.warn("Failed to remove role for Level %s from member %s! %s", String.valueOf(newLevel - 5), String.valueOf(memberId), f.getMessage()));
            e.getGuild().addRoleToMember(UserSnowflake.fromId(e.getMember().getId()), e.getGuild().getRoleById(LevelRoles.getLevelId(newLevel)))
                    .queue(null, f -> Logger.warn("Failed to add role for Level %s to member %s! %s", String.valueOf(newLevel), String.valueOf(memberId), f.getMessage()));
        }
        Bot.database.updateExperience(memberId,newExperience);
    }
}
