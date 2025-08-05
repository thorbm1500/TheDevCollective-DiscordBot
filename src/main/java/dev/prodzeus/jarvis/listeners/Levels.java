package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.configuration.Channels;
import dev.prodzeus.jarvis.configuration.Roles;
import dev.prodzeus.jarvis.member.CollectiveMember;
import dev.prodzeus.jarvis.member.MemberManager;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public final class Levels extends ListenerAdapter {

    private static final Logger LOGGER = SLF4JProvider.get().getLogger("Levels");

    private static final Pattern regex = Pattern.compile(":[^\\s:][^:]*?:");
    private static final List<Long> levels;

    static {
        levels = generateLevels();
    }

    @NotNull
    private static List<Long> generateLevels() {
        final List<Long> levels = new ArrayList<>(101);
        long xp = 0;
        for (int i = 0; i < 101; i++) {
            xp += 5 * ((long) Math.pow(i, 1.75) + 15);
            levels.add(xp);
        }
        return List.copyOf(levels);
    }

    public Levels() {
        LOGGER.debug("New Levels Listener created.");
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
        if (e.isWebhookMessage() || e.getAuthor().isBot() || e.getAuthor().isSystem()) return;

        final long guildId = e.getGuild().getIdLong();
        final CollectiveMember collectiveMember = MemberManager.getCollectiveMember(e.getAuthor().getIdLong(), guildId);
        if (collectiveMember.hasExperienceCooldown()) return;

        final String content = e.getMessage().getContentRaw().toLowerCase();

        long xp = 1;
        xp += Math.clamp((content.length() / 50), 1, 4);

        if (!e.getMessage().getAttachments().isEmpty()) xp += 2;

        long emojis = regex.matcher(content).results().count();
        xp += Math.min(emojis * 2, 8);
        if (e.getMember().isBoosting()) xp *= 2;

        final Pair<Long,Long> expAndLevel = collectiveMember.getData(CollectiveMember.MemberData.EXPERIENCE, CollectiveMember.MemberData.LEVEL);
        final long newExperience = expAndLevel.getLeft() + xp;
        final int newLevel = getLevelFromXp(newExperience);

        if (expAndLevel.getRight() < newLevel) {
            final Collection<Role> add = HashSet.newHashSet(1);
            final Collection<Role> remove = HashSet.newHashSet(1);
            if (newLevel == 1) {
                add.add(Roles.getLevelRole(guildId,1));
            } else if ((newLevel % 5) == 0) {
                remove.add(Roles.getLevelRole(guildId,newLevel == 5 ? 1 : newLevel - 5));
                add.add(Roles.getLevelRole(guildId,newLevel));
            }
            Channels.get(collectiveMember.server)
                    .getChannel(Channels.DevChannel.LEVEL)
                    .sendMessage(Jarvis.getEmojiFormatted("nitro_left_hand")
                                 + " " + collectiveMember.mention
                                 + " is now level **%d** ".formatted(newLevel)
                                 + Jarvis.getEmojiFormatted("nitro_right_hand")
                                 +"\n-# **Current experience** %d ".formatted(newExperience) + Jarvis.getEmojiFormatted("special"))
                    .and(e.getGuild().modifyMemberRoles(e.getMember(),add,remove))
                    .queue();
        }
        collectiveMember.updateExperienceAndLevel(newLevel, newExperience);
    }
}
