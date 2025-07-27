package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.configuration.enums.Channel;
import dev.prodzeus.jarvis.configuration.enums.LevelRoles;
import dev.prodzeus.jarvis.member.CollectiveMember;
import dev.prodzeus.jarvis.member.MemberManager;
import dev.prodzeus.jarvis.utils.Utils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Levels extends ListenerAdapter {

    private static final String confetti = Jarvis.BOT.getEmojiFormatted("confetti");

    private static final Pattern regex = Pattern.compile(":[^\\s:][^:]*?:");
    private static final List<Long> levels;

    static {
        levels = generateLevels();
    }

    @NotNull
    private static synchronized List<Long> generateLevels() {
        final List<Long> levels = new ArrayList<>(101);
        long xp = 0;
        for (int i = 0; i < 101; i++) {
            xp += 5 * ((long) Math.pow(i, 1.75) + 15);
            levels.add(xp);
        }
        return List.copyOf(levels);
    }

    public Levels() {
        Jarvis.LOGGER.debug("New Levels Listener created.");
    }

    public static int getLevelFromXp(final long xp) {
        int level = 0;
        for (final long req : levels) {
            if (xp > req) level++;
            else return level;
        }
        return 0;
    }

    @Override
    public void onMessageReceived(@NotNull final MessageReceivedEvent e) {
        if (e.isWebhookMessage() || !Utils.isUser(e.getAuthor())) return;

        final CollectiveMember collectiveMember = MemberManager.getCollectiveMember(e.getAuthor().getIdLong(), e.getGuild().getIdLong());
        if (collectiveMember.isOnCooldown()) return;

        final String content = e.getMessage().getContentRaw().toLowerCase();

        long xp = 1;
        xp += Math.clamp((content.length()/50),1,4);

        if (!e.getMessage().getAttachments().isEmpty()) xp += 1;

        long emojis = regex.matcher(content).results().count();
        xp += Math.min(emojis * 2, 8);
        if (e.getMember().isBoosting()) xp *= 2;

        final long newExperience = collectiveMember.getExperience() + xp;
        final int newLevel = getLevelFromXp(newExperience);

        if (collectiveMember.getLevel() < newLevel) {
            Utils.getTextChannel(Channel.LEVEL.id).sendMessage(content+" %s is now level: **%d**\n-# Current experience: %d"
                    .formatted(collectiveMember,newLevel,newExperience))
                    .queue();
            if (newLevel == 1) collectiveMember.addRole(LevelRoles.LEVEL_1);
            else if ((newLevel % 5) == 0) {
                collectiveMember.removeRole(LevelRoles.getLevelId(newLevel - 5));
                collectiveMember.addRole(LevelRoles.getLevelId(newLevel));
            }
        }
        collectiveMember.updateExperienceAndLevel(newLevel, newExperience);
    }
}
