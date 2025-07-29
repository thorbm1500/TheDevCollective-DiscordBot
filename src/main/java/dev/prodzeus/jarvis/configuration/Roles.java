package dev.prodzeus.jarvis.configuration;

import dev.prodzeus.jarvis.bot.Jarvis;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Roles {

    private static final Map<Long, Roles> instances = new HashMap<>();

    private final long id;

    public long member = 0L;
    public long staff = 0L;
    public long level_1 = 0L;
    public long level_5 = 0L;
    public long level_10 = 0L;
    public long level_15 = 0L;
    public long level_20 = 0L;
    public long level_25 = 0L;
    public long level_30 = 0L;
    public long level_35 = 0L;
    public long level_40 = 0L;
    public long level_45 = 0L;
    public long level_50 = 0L;
    public long level_55 = 0L;
    public long level_60 = 0L;
    public long level_65 = 0L;
    public long level_70 = 0L;
    public long level_75 = 0L;
    public long level_80 = 0L;
    public long level_85 = 0L;
    public long level_90 = 0L;
    public long level_95 = 0L;
    public long level_100 = 0L;

    private Roles(final long serverId) {
        if (instances.containsKey(serverId)) {
            throw new IllegalStateException("Roles instance already exists for server %d!".formatted(serverId));
        }
        this.id = serverId;
        final Roles.RoleIds ids = Jarvis.DATABASE.getRoleIds(serverId);
        if (ids.member != null) this.member = ids.member;
        if (ids.staff != null) this.staff = ids.staff;
        if (ids.level_1 != null) this.level_1 = ids.level_1;
        if (ids.level_5 != null) this.level_5 = ids.level_5;
        if (ids.level_10 != null) this.level_10 = ids.level_10;
        if (ids.level_15 != null) this.level_15 = ids.level_15;
        if (ids.level_20 != null) this.level_20 = ids.level_20;
        if (ids.level_25 != null) this.level_25 = ids.level_25;
        if (ids.level_30 != null) this.level_30 = ids.level_30;
        if (ids.level_35 != null) this.level_35 = ids.level_35;
        if (ids.level_40 != null) this.level_40 = ids.level_40;
        if (ids.level_45 != null) this.level_45 = ids.level_45;
        if (ids.level_50 != null) this.level_50 = ids.level_50;
        if (ids.level_55 != null) this.level_55 = ids.level_55;
        if (ids.level_60 != null) this.level_60 = ids.level_60;
        if (ids.level_65 != null) this.level_65 = ids.level_65;
        if (ids.level_70 != null) this.level_70 = ids.level_70;
        if (ids.level_75 != null) this.level_75 = ids.level_75;
        if (ids.level_80 != null) this.level_80 = ids.level_80;
        if (ids.level_85 != null) this.level_85 = ids.level_85;
        if (ids.level_90 != null) this.level_90 = ids.level_90;
        if (ids.level_95 != null) this.level_95 = ids.level_95;
        if (ids.level_100 != null) this.level_100 = ids.level_100;
        instances.put(this.id,this);
    }

    public void update(@NotNull final String role, final long id) {
        update(role,id,false);
    }

    public void update(@NotNull final String role, final long id, final boolean holdUpdate) {
        switch (role.toLowerCase()) {
            case "member" -> this.member = id;
            case "staff" -> this.staff = id;
            case "level_1" -> this.level_1 = id;
            case "level_5" -> this.level_5 = id;
            case "level_10" -> this.level_10 = id;
            case "level_15" -> this.level_15 = id;
            case "level_20" -> this.level_20 = id;
            case "level_25" -> this.level_25 = id;
            case "level_30" -> this.level_30 = id;
            case "level_35" -> this.level_35 = id;
            case "level_40" -> this.level_40 = id;
            case "level_45" -> this.level_45 = id;
            case "level_50" -> this.level_50 = id;
            case "level_55" -> this.level_55 = id;
            case "level_60" -> this.level_60 = id;
            case "level_65" -> this.level_65 = id;
            case "level_70" -> this.level_70 = id;
            case "level_75" -> this.level_75 = id;
            case "level_80" -> this.level_80 = id;
            case "level_85" -> this.level_85 = id;
            case "level_90" -> this.level_90 = id;
            case "level_95" -> this.level_95 = id;
            case "level_100" -> this.level_100 = id;
            default -> {
                Jarvis.LOGGER.error("Attempted to update ID for Role {}, but no such Role exists!", role);
                return;
            }
        }
        if (!holdUpdate) updateDatabase();
    }

    public void updateDatabase() {
        Jarvis.DATABASE.saveRoleIds(getRoles());
    }

    public static Roles get(final long serverId) {
        if (instances.containsKey(serverId)) return instances.get(serverId);
        else return instances.put(serverId, new Roles(serverId));
    }

    public static RoleIds getRoles(final long serverId) {
        return get(serverId).getRoles();
    }

    public RoleIds getRoles() {
        return new RoleIds(id, member, staff, level_1, level_5, level_10,
                level_15, level_20, level_25, level_30, level_35, level_40,
                level_45, level_50, level_55, level_60, level_65, level_70,
                level_75, level_80, level_85, level_90, level_95, level_100);
    }

    public record RoleIds(long id, Long member, Long staff,
                          Long level_1, Long level_5, Long level_10,
                          Long level_15, Long level_20, Long level_25,
                          Long level_30, Long level_35, Long level_40,
                          Long level_45, Long level_50, Long level_55,
                          Long level_60, Long level_65, Long level_70,
                          Long level_75, Long level_80, Long level_85,
                          Long level_90, Long level_95, Long level_100) {}
}
