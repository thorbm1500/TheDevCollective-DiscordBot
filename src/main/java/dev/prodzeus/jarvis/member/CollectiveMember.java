package dev.prodzeus.jarvis.member;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.configuration.enums.LevelRoles;
import dev.prodzeus.jarvis.configuration.enums.Roles;
import dev.prodzeus.jarvis.enums.Counts;
import dev.prodzeus.jarvis.listeners.Levels;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.MiscUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Formattable;
import java.util.FormattableFlags;
import java.util.Formatter;

@SuppressWarnings("unused")
public class CollectiveMember implements Formattable {

    public final long id;
    public final String mention;
    public final long server;

    private int level;
    private long experience;
    private long latestExperienceUpdate = 0L;

    private String countLevel;
    private int correctCounts;
    private int incorrectCounts;

    private long lastUpdate;

    public CollectiveMember(final long id, final long server) {
        Jarvis.DATABASE.addMember(id,server);
        this.id = id;
        this.mention = "<@"+id+">";
        this.server = server;
        this.level = Jarvis.DATABASE.getLevel(id,server);
        this.experience = Jarvis.DATABASE.getExperience(id,server);

        final Counts counts = Jarvis.DATABASE.getUserCounts(id,server);
        this.countLevel = counts.level();
        this.correctCounts = counts.correctCounts();
        this.incorrectCounts = counts.incorrectCounts();

        Jarvis.LOGGER.debug("New Collective Member instance created for {} in server {}",id,server);
        validate();
        update();
    }

    private long update() {
        lastUpdate = System.currentTimeMillis();
        return lastUpdate;
    }

    public long getTimeOfLastUpdate() {
        return lastUpdate;
    }

    public boolean isInactive() {
        return (System.currentTimeMillis() - lastUpdate) > 300000;
    }

    public boolean isInactive(final long time) {
        return (time - lastUpdate) > 300000;
    }

    private void validate() {
        final int expectedLevel = Levels.getLevelFromXp(experience);
        if (level == expectedLevel) {
            Jarvis.LOGGER.debug("Data validated for member {} in server {}. No errors found.",id,server);
        } else {
            Jarvis.LOGGER.info("Wrong level found for member {} in server {}. Expected Level: {}. Level Found: {}. Updating...", id, server, expectedLevel, level);
            level = expectedLevel;
            Jarvis.DATABASE.updateLevel(id, server, level);
            validateAndRepairLevelRoles();
        }
    }

    private void validateAndRepairLevelRoles() {
        try {
            Jarvis.LOGGER.debug("Repairing level roles for member {}...",id);
            if (!getMember().canInteract(Jarvis.BOT.jda.getGuildById(server).getMember(Jarvis.BOT.jda.getSelfUser()))) return;
            getMember().getRoles().forEach(role -> {
                if (LevelRoles.contains(role.getIdLong())) removeRole(role.getIdLong());
            });
            if (level > 0) {
                final LevelRoles role = LevelRoles.getLevelRole(level - (level % 5));
                addRole(role);
            }
            Jarvis.LOGGER.debug("Roles successfully repaired and validated for member {}");
        } catch (Exception e) {
            Jarvis.LOGGER.error("Exception thrown while attempting to repair and validate level roles for member {}! {}",id,e);
        }
    }

    @Nullable
    public Member getMember() {
        update();
        try {
            return Jarvis.BOT.jda.getGuildById(server).getMemberById(id);
        } catch (Exception e) {
            Jarvis.LOGGER.warn("Failed to get member instance for member {} in server {}! {}",id,server,e);
            return null;
        }
    }

    @Nullable
    public User getUser() {
        update();
        return Jarvis.BOT.jda.getUserById(id);
    }

    public int getLevel() {
        update();
        return level;
    }

    public void updateLevel(final int level) {
        update();
        if (this.level == level) return;
        this.level = level;
        Jarvis.DATABASE.updateLevel(id,server,level);
    }

    public long getExperience() {
        update();
        return experience;
    }

    public void updateExperience(final long experience) {
        latestExperienceUpdate = update();
        if (this.experience == experience) return;
        this.experience = experience;
        Jarvis.DATABASE.updateExperience(id,server,experience);
    }

    public void updateExperienceAndLevel(final int level, final long experience) {
        updateExperience(experience);
        updateLevel(level);
    }

    public boolean isOnCooldown() {
        update();
        return ((latestExperienceUpdate / 1000) - lastUpdate) < 30;
    }

    public String getCountLevel() {
        update();
        return countLevel;
    }

    public void updateCountLevel(@NotNull final String countLevel) {
        update();
        this.countLevel = countLevel;
    }

    public int getCorrectCounts() {
        update();
        return correctCounts;
    }

    public void incrementCorrectCounts() {
        update();
        correctCounts++;
        Jarvis.DATABASE.incrementCorrectCount(id,server);
    }

    public int getIncorrectCounts() {
        update();
        return incorrectCounts;
    }

    public void incrementIncorrectCounts() {
        update();
        incorrectCounts++;
        Jarvis.DATABASE.incrementIncorrectCount(id,server);
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
                    .queue(null, f -> Jarvis.LOGGER.error("Failed to add role {} to member {}! {}", roleId, id, f));
        } catch (Exception e) {
            Jarvis.LOGGER.error("Failed to remove role {} from member {}! {}",roleId,id,e);
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
                    .queue(null, f -> Jarvis.LOGGER.error("Failed to add role {} to member {}! {}", roleId, id, f));
        } catch (Exception e) {
            Jarvis.LOGGER.error("Failed to add role {} from member {}! {}",roleId,id,e);
        }
    }

    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision) {
        final boolean leftJustified = (flags & FormattableFlags.LEFT_JUSTIFY) == FormattableFlags.LEFT_JUSTIFY;
        final boolean upper = (flags & FormattableFlags.UPPERCASE) == FormattableFlags.UPPERCASE;
        MiscUtil.appendTo(formatter, width, precision, leftJustified, mention);
    }
}
