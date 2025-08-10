package dev.prodzeus.jarvis.member;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.configuration.Roles;
import dev.prodzeus.jarvis.enums.MemberCredentials;
import dev.prodzeus.jarvis.games.count.CountLevel;
import dev.prodzeus.jarvis.listeners.Levels;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static dev.prodzeus.jarvis.bot.Jarvis.LOGGER;

@SuppressWarnings("unused")
public class CollectiveMember implements Formattable {

    private int unsavedChanges = 0;

    private final EnumMap<MemberData,Long> data;
    public final long id;
    public final long server;
    public final String mention;
    private long experienceCooldown = 0;
    private final List<Role> roles = new ArrayList<>(8);

    private boolean isActive = false;
    private CountLevel countLevel = CountLevel.LEVEL_0;
    private long nextCountLevelRequirement = countLevel.requirement;
    private final Set<Long> reactionsGiven = new HashSet<>();
    private final Set<Long> reactionsReceived = new HashSet<>();

    public enum MemberData {
        LEVEL, EXPERIENCE,
        CORRECT_COUNTS, INCORRECT_COUNTS,
        IMAGES_SENT, REACTIONS_GIVEN, REACTIONS_RECEIVED
    }

    public CollectiveMember(final long server, final long id) {
        this.id = id;
        this.server = server;
        this.mention = "<@"+id+">";
        data = Jarvis.DATABASE.loadMember(new MemberCredentials(server,id));

        LOGGER.debug("[Server:{}] [Member:{}] New Collective Member instance created.",server,id);
        validate();
        confirmActivity();
    }

    public void save() {
        Jarvis.DATABASE.saveMember(this);
        unsavedChanges = 0;
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
        final long expectedLevel = Levels.getLevelFromXp(getData(MemberData.EXPERIENCE));
        final long level = getData(MemberData.LEVEL);
        if (level == expectedLevel) {
            LOGGER.debug("[Server:{}] [Member:{}] Data validated. No errors found.",id,server);
        } else {
            LOGGER.info("[Server:{}] [Member:{}] Wrong level found. Expected Level: {}. Level Found: {}. Updating...", id, server, expectedLevel, level);
            updateData(MemberData.LEVEL,expectedLevel);
            validateAndRepairLevelRoles();
        }
    }

    private void validateAndRepairLevelRoles() {
        try {
            LOGGER.debug("[Server:{}] [Member:{}] Repairing level roles...",server,id);
            final Member member = getMember();
            if (member == null) {
                LOGGER.warn("[Server:{}] [Member:{}] Failed to repair level roles. Member is null!" ,server,id);
                return;
            }
            for (final Role role : member.getRoles()) {
                if (role.getName().toLowerCase().contains("level")) removeRole(role.getIdLong());
            }
            final long level = getData(MemberData.LEVEL);
            if (level > 0) {
                final Role role = Roles.getLevelRole(server, level);
                if (role != null) addRole(role);
            }
            LOGGER.debug("Roles successfully repaired and validated for member {}");
        } catch (Exception e) {
            LOGGER.error("[Server:{}] [Member:{}] Failed to repair and validate level roles. {}",server,id,e);
        }
    }

    @Contract(pure = true)
    public @NotNull EnumMap<MemberData,Long> getCurrentData() {
        return new EnumMap<>(data);
    }

    public long getData(@NotNull final MemberData data) {
        confirmActivity();
        return this.data.getOrDefault(data,0L);
    }

    public Pair<Long,Long> getData(@NotNull final MemberData d1, @NotNull final MemberData d2) {
        return Pair.of(getData(d1),getData(d2));
    }

    private void updateData(@NotNull final Map<MemberData,Long> entries) {
        confirmActivity();
        if (entries.containsKey(MemberData.EXPERIENCE)) experienceCooldown = System.currentTimeMillis();
        synchronized (data) {
            data.putAll(entries);
        }
        if (unsavedChanges++ > 15) save();
    }

    private void updateData(@NotNull final MemberData data, final long value) {
        updateData(Map.of(data,value));
    }

    private void updateData(@NotNull final MemberData d1, final long v1, @NotNull final MemberData d2, final long v2) {
        updateData(Map.of(d1,v1,d2,v2));
    }

    private void updateData(@NotNull final MemberData d1, final long v1, @NotNull final MemberData d2, final long v2, @NotNull final MemberData d3, final long v3) {
        updateData(Map.of(d1,v1,d2,v2,d3,v3));
    }

    public @Nullable Member getMember() {
        confirmActivity();
        return Jarvis.getMember(server,id);
    }

    public @Nullable User getUser() {
        confirmActivity();
        return Jarvis.getUser(id);
    }

    public @NotNull List<Role> getRoles() {
        if (roles.isEmpty()) roles.addAll(getMember().getRoles());
        confirmActivity();
        return roles;
    }

    public String getCountLevelIcon() {
        confirmActivity();
        return countLevel.getEmoji();
    }

    public void updateExperienceAndLevel(final int level, final long experience) {
        updateData(MemberData.LEVEL,level,MemberData.EXPERIENCE,experience);
    }

    public boolean hasExperienceCooldown() {
        return (System.currentTimeMillis() - experienceCooldown) < 30000;
    }

    public void increment(@NotNull final MemberData data) {
        increment(data,1);
    }

    public void increment(@NotNull final MemberData data, final long amount) {
        updateData(data,getData(data)+amount);
        if (data==MemberData.CORRECT_COUNTS) updateCountStats();
    }

    private int countUpdates = 0;
    public void updateCountStats() {
        if (countUpdates++ > 5 || getData(MemberData.CORRECT_COUNTS) > nextCountLevelRequirement) {
            countLevel = CountLevel.getCountLevel(getData(MemberData.CORRECT_COUNTS));
            nextCountLevelRequirement = countLevel.requirement;
        }
    }

    public void removeRole(@NotNull final Roles.DevRole role) {
        removeRole(role.getRoleId(server));
    }

    public void removeRole(final long roleId) {
        removeRole(Roles.getRole(roleId));
    }

    public void removeRole(@NotNull final Role role) {
        confirmActivity();
        try {
            Jarvis.jda().getGuildById(server)
                    .removeRoleFromMember(getMember(), role)
                    .queue(null,
                            f -> LOGGER.error("Failed to add role {} to member {}! {}", role.getName(), id, f));
        } catch (Exception e) {
            LOGGER.error("Failed to remove role {} from member {}! {}",role.getName(),id,e);
        }
    }

    public void addRole(@NotNull final Roles.DevRole role) {
        addRole(role.getRoleId(server));
    }

    public void addRole(final long roleId) {
        addRole(Roles.getRole(roleId));
    }

    public void addRole(@NotNull final Role role) {
        confirmActivity();
        try {
            Jarvis.jda().getGuildById(server)
                    .addRoleToMember(getMember(), role)
                    .queue(null,
                            f -> LOGGER.error("Failed to add role {} to member {}! {}", role.getName(), id, f));
        } catch (Exception e) {
            LOGGER.error("Failed to add role {} to member {}! {}",role.getName(),id,e);
        }
    }

    public static void updateRoles(final long serverId, final long memberId, @NotNull final Role add, @NotNull final Role remove) {
        updateRoles(serverId,memberId,Set.of(add),Set.of(remove));
    }

    public static void updateRoles(final long serverId, final long memberId, @NotNull final Collection<Role> add, @NotNull final Collection<Role> remove) {
        MemberManager.getCollectiveMember(serverId,memberId).updateRoles(add,remove);
    }

    public void updateRoles(@NotNull final Role add, @NotNull final Role remove) {
        updateRoles(Set.of(add),Set.of(remove));
    }

    public void updateRoles(@NotNull final Collection<Role> add, @NotNull final Collection<Role> remove) {
        confirmActivity();
        try {
            Jarvis.getGuild(server)
                    .modifyMemberRoles(getMember(),add,remove)
                    .queue(null,
                            f -> LOGGER.error("[Server:{}] [Member:{}] Failed to modify roles, Add: {} & Remove: {}! {}", server, id, add, remove, f));
        } catch (Exception e) {
            LOGGER.error("[Server:{}] [Member:{}] Failed to modify roles, Add: {} & Remove: {}! {}", server, id, add, remove, e);
        }
    }

    public void handleGiveReaction(final long messageId) {
        if (hasReacted(messageId)) return;
        reactionsGiven.add(messageId);
        increment(MemberData.EXPERIENCE, 2);
    }

    public void handleReceivedReaction(final long messageId) {
        if (hasReceived(messageId)) return;
        reactionsReceived.add(messageId);
        increment(MemberData.EXPERIENCE, 2);
    }

    public boolean hasReacted(final long messageId) {
        return reactionsGiven.contains(messageId);
    }

    public boolean hasReceived(final long messageId) {
        return reactionsReceived.contains(messageId);
    }

    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision) {
        final boolean leftJustified = (flags & FormattableFlags.LEFT_JUSTIFY) == FormattableFlags.LEFT_JUSTIFY;
        final boolean upper = (flags & FormattableFlags.UPPERCASE) == FormattableFlags.UPPERCASE;
        MiscUtil.appendTo(formatter, width, precision, leftJustified, mention);
    }
}
