package dev.prodzeus.jarvis.member;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.configuration.enums.LevelRoles;
import dev.prodzeus.jarvis.configuration.enums.Roles;
import dev.prodzeus.jarvis.enums.Counts;
import dev.prodzeus.jarvis.games.count.CountLevel;
import dev.prodzeus.jarvis.listeners.Levels;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.MiscUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Formattable;
import java.util.FormattableFlags;
import java.util.Formatter;

import static dev.prodzeus.jarvis.bot.Jarvis.LOGGER;

@SuppressWarnings("unused")
public class CollectiveMember implements Formattable {

    public final long id;
    public final String mention;
    public final long server;

    private int level;
    private long experience;
    private long latestExperienceUpdate = 0L;

    private int countLevel;
    private String countLevelIcon;
    private int nextLevelRequirement;
    private int correctCounts;
    private int incorrectCounts;

    private boolean isActive;

    public CollectiveMember(final long id, final long server) {
        Jarvis.DATABASE.addMember(id,server);
        this.id = id;
        this.mention = "<@"+id+">";
        this.server = server;
        this.level = Jarvis.DATABASE.getLevel(id,server);
        this.experience = Jarvis.DATABASE.getExperience(id,server);

        final Counts counts = Jarvis.DATABASE.getUserCounts(id,server);
        this.countLevel = counts.level();
        this.countLevelIcon = counts.levelIcon();
        this.nextLevelRequirement = CountLevel.getNextLevelRequirement(counts.level());
        this.correctCounts = counts.correctCounts();
        this.incorrectCounts = counts.incorrectCounts();

        LOGGER.debug("New Collective Member instance created for {} in server {}",id,server);
        validate();
        confirmActivity();
    }

    private synchronized void confirmActivity() {
        isActive = true;
    }

    public boolean isInactive() {
        return !isActive;
    }

    public synchronized void resetActivity() {
        isActive = false;
    }

    private void validate() {
        final int expectedLevel = Levels.getLevelFromXp(experience);
        if (level == expectedLevel) {
            LOGGER.debug("Data validated for member {} in server {}. No errors found.",id,server);
        } else {
            LOGGER.info("Wrong level found for member {} in server {}. Expected Level: {}. Level Found: {}. Updating...", id, server, expectedLevel, level);
            level = expectedLevel;
            Jarvis.DATABASE.updateLevel(id, server, level);
            validateAndRepairLevelRoles();
        }
    }

    private void validateAndRepairLevelRoles() {
        try {
            LOGGER.debug("Repairing level roles for member {}...",id);
            final Member member = getMember();
            for (final Role role : member.getRoles()) {
                if (LevelRoles.contains(role.getIdLong())) removeRole(role.getIdLong());
            }
            if (level > 0) {
                final LevelRoles role = level < 5 ? LevelRoles.LEVEL_1 : LevelRoles.getLevelRole(level - (level % 5));
                addRole(role);
            }
            LOGGER.debug("Roles successfully repaired and validated for member {}");
        } catch (Exception e) {
            LOGGER.error("Exception thrown while attempting to repair and validate level roles for member {}! {}",id,e);
        }
    }

    @Nullable
    public Member getMember() {
        confirmActivity();
        try {
            return Jarvis.BOT.jda.getGuildById(server).getMemberById(id);
        } catch (Exception e) {
            LOGGER.warn("Failed to get member instance for member {} in server {}! {}",id,server,e);
            return null;
        }
    }

    @Nullable
    public User getUser() {
        confirmActivity();
        return Jarvis.jda().getUserById(id);
    }

    public int getLevel() {
        confirmActivity();
        return level;
    }

    public void updateLevel(final int level) {
        confirmActivity();
        synchronized (this) {
            if (this.level == level) return;
            this.level = level;
        }
        Jarvis.DATABASE.updateLevel(id,server,level);
    }

    public long getExperience() {
        confirmActivity();
        return experience;
    }

    public void updateExperience(final long experience) {
        confirmActivity();
        synchronized (this) {
            latestExperienceUpdate = System.currentTimeMillis();
            if (this.experience == experience) return;
            this.experience = experience;
        }
        Jarvis.DATABASE.updateExperience(id,server,experience);
    }

    public void updateExperienceAndLevel(final int level, final long experience) {
        updateExperience(experience);
        updateLevel(level);
    }

    public boolean isOnCooldown() {
        confirmActivity();
        return (System.currentTimeMillis() - latestExperienceUpdate) < 30000;
    }

    public int getCountLevel() {
        confirmActivity();
        return countLevel;
    }

    public String getCountLevelIcon() {
        confirmActivity();
        return countLevelIcon;
    }

    public int getCorrectCounts() {
        confirmActivity();
        return correctCounts;
    }

    public int getIncorrectCounts() {
        confirmActivity();
        return incorrectCounts;
    }

    public void incrementCorrectCounts() {
        confirmActivity();
        synchronized (this) {
            correctCounts++;
        }
        Jarvis.DATABASE.incrementCorrectCount(id, server);
        updateCountStats();
    }

    public void incrementIncorrectCounts() {
        confirmActivity();
        synchronized (this) {
            incorrectCounts++;
        }
        Jarvis.DATABASE.incrementIncorrectCount(id,server);
    }

    public void updateCountStats() {
        if (this.correctCounts > nextLevelRequirement) {
            confirmActivity();
            synchronized (this) {
                this.countLevel++;
                this.countLevelIcon = Counts.levelIcon(this.countLevel);
                this.nextLevelRequirement = CountLevel.getNextLevelRequirement(level);
            }
        }
    }

    public void removeRole(@NotNull final Roles role) {
        removeRole(role.id);
    }

    public void removeRole(@NotNull final LevelRoles role) {
        removeRole(role.id);
    }

    public void removeRole(final long roleId) {
        try {
            Jarvis.BOT.jda.getGuildById(server)
                    .removeRoleFromMember(getMember(), Jarvis.BOT.jda.getRoleById(roleId))
                    .queue(null, f -> LOGGER.error("Failed to add role {} to member {}! {}", roleId, id, f));
        } catch (Exception e) {
            LOGGER.error("Failed to remove role {} from member {}! {}",roleId,id,e);
        }
    }

    public void addRole(@NotNull final Roles role) {
        addRole(role.id);
    }

    public void addRole(@NotNull final LevelRoles role) {
        addRole(role.id);
    }

    public void addRole(final long roleId) {
        try {
            Jarvis.BOT.jda.getGuildById(server)
                    .addRoleToMember(getMember(), Jarvis.BOT.jda.getRoleById(roleId))
                    .queue(null, f -> LOGGER.error("Failed to add role {} to member {}! {}", roleId, id, f));
        } catch (Exception e) {
            LOGGER.error("Failed to add role {} from member {}! {}",roleId,id,e);
        }
    }

    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision) {
        final boolean leftJustified = (flags & FormattableFlags.LEFT_JUSTIFY) == FormattableFlags.LEFT_JUSTIFY;
        final boolean upper = (flags & FormattableFlags.UPPERCASE) == FormattableFlags.UPPERCASE;
        MiscUtil.appendTo(formatter, width, precision, leftJustified, mention);
    }
}
