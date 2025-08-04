package dev.prodzeus.jarvis.configuration;

import dev.prodzeus.jarvis.bot.Jarvis;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static dev.prodzeus.jarvis.configuration.Roles.DevRole.*;

public class Roles {

    private static final Map<Long, Roles> instances = new HashMap<>();

    private final long id;
    private final EnumMap<DevRole,Long> roles;
    private static final HashMap<Long,DevRole> LEVEL_ROLES = HashMap.newHashMap(21);
    static {
        LEVEL_ROLES.put(1L,LEVEL_1);
        LEVEL_ROLES.put(5L,LEVEL_5);
        LEVEL_ROLES.put(10L,LEVEL_10);
        LEVEL_ROLES.put(15L,LEVEL_15);
        LEVEL_ROLES.put(20L,LEVEL_20);
        LEVEL_ROLES.put(25L,LEVEL_25);
        LEVEL_ROLES.put(30L,LEVEL_30);
        LEVEL_ROLES.put(35L,LEVEL_35);
        LEVEL_ROLES.put(40L,LEVEL_40);
        LEVEL_ROLES.put(45L,LEVEL_45);
        LEVEL_ROLES.put(50L,LEVEL_50);
        LEVEL_ROLES.put(55L,LEVEL_55);
        LEVEL_ROLES.put(60L,LEVEL_60);
        LEVEL_ROLES.put(65L,LEVEL_65);
        LEVEL_ROLES.put(70L,LEVEL_70);
        LEVEL_ROLES.put(75L,LEVEL_75);
        LEVEL_ROLES.put(80L,LEVEL_80);
        LEVEL_ROLES.put(85L,LEVEL_85);
        LEVEL_ROLES.put(90L,LEVEL_90);
        LEVEL_ROLES.put(95L,LEVEL_95);
        LEVEL_ROLES.put(100L,LEVEL_100);
    }

    public enum DevRole {
        MEMBER,STAFF,
        LEVEL_1,LEVEL_5,LEVEL_10,
        LEVEL_15,LEVEL_20,LEVEL_25,
        LEVEL_30,LEVEL_35,LEVEL_40,
        LEVEL_45,LEVEL_50,LEVEL_55,
        LEVEL_60,LEVEL_65,LEVEL_70,
        LEVEL_75,LEVEL_80,LEVEL_85,
        LEVEL_90,LEVEL_95,LEVEL_100
        ;

        public static @Nullable DevRole of(@NotNull final String value) {
            try {
                return DevRole.valueOf(value);
            } catch (Exception ignored) {
                return null;
            }
        }

        public long getRoleId(final long serverId) {
            return Roles.get(serverId).getRoleId(this);
        }
    }

    private Roles(final long serverId) {
        if (instances.containsKey(serverId)) {
            throw new IllegalStateException("Roles instance already exists for server %d!".formatted(serverId));
        }
        this.id = serverId;
        roles = Jarvis.DATABASE.getRoleIds(this.id);
        instances.put(this.id,this);
    }

    public boolean save() {
        return Jarvis.DATABASE.saveRoleIds(this.id,roles);
    }

    public static boolean update(final long serverId, @NotNull final EnumMap<DevRole,Long> updates) {
        return get(serverId).update(updates);
    }

    public boolean update(@NotNull final EnumMap<DevRole,Long> updates) {
        roles.putAll(updates);
        return save();
    }

    public static boolean update(final long serverId, @NotNull final DevRole role, final long id) {
        return get(serverId).update(role,id);
    }

    public boolean update(@NotNull final DevRole role, final long id) {
        roles.put(role,id);
        return Jarvis.DATABASE.updateRoleId(this.id,role,id);
    }

    @Contract(pure = true)
    public static long getRoleId(final long serverId, @NotNull final DevRole role) {
        return Roles.get(serverId).getRoleId(role);
    }

    @Contract(pure = true)
    public long getRoleId(@NotNull final DevRole role) {
        return roles.getOrDefault(role,0L);
    }

    public @Nullable Role getRole(@NotNull final DevRole role) {
        return getRole(getRoleId(role));
    }

    public static @Nullable Role getRole(final long id) {
        return Jarvis.jda().getRoleById(id);
    }

    @Contract(pure = true)
    public @Nullable Role getLevelRole(final long level) {
        if (LEVEL_ROLES.containsKey(level)) {
            return getRole(LEVEL_ROLES.get(level));
        }
        return null;
    }

    @Contract(pure = true)
    public static @Nullable Role getLevelRole(final long serverId, final long level) {
        return Roles.get(serverId).getLevelRole(level);
    }

    public static @NotNull Roles get(final long serverId) {
        if (instances.containsKey(serverId)) return instances.get(serverId);
        else return instances.put(serverId, new Roles(serverId));
    }
}
