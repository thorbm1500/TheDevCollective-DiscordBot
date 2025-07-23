package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.configuration.enums.Channel;
import dev.prodzeus.jarvis.configuration.enums.LevelRoles;
import dev.prodzeus.jarvis.enums.Emoji;
import dev.prodzeus.jarvis.enums.Member;
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

import static java.util.logging.Level.WARNING;

@SuppressWarnings("unused")
public class Levels extends ListenerAdapter {
    private static final Pattern emoji = Pattern.compile(":[^\\s:][^:]*?:");
    private static final long[] levels = new long[100];
    private static final HashMap<Long, Long> experienceCooldown = new HashMap<>();

    public Levels() {
        long xp = 0;
        for (int i = 0; i < 100; i++) {
            xp += 5 * ((long) Math.pow(i, 1.75) + 15);
            levels[i] = xp;
        }
    }

    private int getLevelFromXp(final long xp) {
        if (xp < levels[0]) return 0;
        int index = Arrays.binarySearch(levels, xp);
        if (xp == levels[index]) return index;
        else return index - 1;
    }

    @SneakyThrows
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        final Member member = Utils.getMember(e.getMember());
        final long timeOfCreation = e.getMessage().getTimeCreated().toEpochSecond();
        if (e.getAuthor().isBot() || e.getAuthor().isSystem() || e.isWebhookMessage()) return;
        if (((experienceCooldown.getOrDefault(member.id(), 10000000000L) / 1000) - timeOfCreation) < 30) return;
        else experienceCooldown.put(member.id(), timeOfCreation);

        final long currentExperience = Bot.database.getExperience(member);
        final int currentLevel = getLevelFromXp(currentExperience);

        long xp = 1;

        String content = e.getMessage().getContentRaw();
        xp += Math.min(content.length(), 200) / 50;

        if (!e.getMessage().getAttachments().isEmpty()) xp += 1;

        long emojis = emoji.matcher(content).results().count();
        emojis -= (emojis + 1) % 2;
        xp += Math.min(emojis, 5) - 1;

        final long newExperience = currentExperience + xp;
        final int newLevel = getLevelFromXp(newExperience);

        final MessageChannel channel = Utils.getTextChannel(Channel.LEVEL.id);
        if (currentLevel < newLevel) {
            channel.sendMessage("%s %s  is now level: **%d**\n-# Current experience: %d"
                    .formatted(Emoji.CONFETTI.id, e.getMember().getAsMention(),newLevel,newExperience)).queue();

            if (newLevel == 1) {
                e.getGuild().addRoleToMember(UserSnowflake.fromId(e.getMember().getIdLong()), LevelRoles.LEVEL_1.getRole())
                        .queue(null, f -> Logger.log(WARNING,"Failed to add role for Level 1 to member %s! %s",member.id(),f));
                return;
            }

            if (newLevel % 5 != 0) return;
            e.getGuild().removeRoleFromMember(UserSnowflake.fromId(e.getMember().getIdLong()), LevelRoles.getRole(newLevel-5))
                    .queue(null, f -> Logger.log(WARNING,"Failed to remove role for Level %s from member %s! %s",(newLevel - 5),member.id(),f));
            e.getGuild().addRoleToMember(UserSnowflake.fromId(e.getMember().getIdLong()), LevelRoles.getRole(newLevel))
                    .queue(null, f -> Logger.log(WARNING,"Failed to add role for Level %s to member %s! %s",newLevel,member.id(),f));
        }
        Bot.database.updateExperience(member,newExperience);
    }
}
