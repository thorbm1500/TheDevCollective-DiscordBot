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
        //todo: Add logic.
    }

    public static Roles get(final long serverId) {
        return instances.computeIfAbsent(serverId, Roles::new);
    }

    public static RoleIds getRoles(final long serverId) {
        final Roles roles = get(serverId);
        return new RoleIds(roles.id, roles.member, roles.staff, roles.level_1, roles.level_5, roles.level_10,
                roles.level_15, roles.level_20, roles.level_25, roles.level_30, roles.level_35, roles.level_40,
                roles.level_45, roles.level_50, roles.level_55, roles.level_60, roles.level_65, roles.level_70,
                roles.level_75, roles.level_80, roles.level_85, roles.level_90, roles.level_95, roles.level_100);
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
