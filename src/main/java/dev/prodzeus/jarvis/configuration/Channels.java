package dev.prodzeus.jarvis.configuration;

import dev.prodzeus.jarvis.bot.Jarvis;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class Channels {

    private static final Map<Long, Channels> instances = new HashMap<>();

    private final long id;
    private final EnumMap<DevChannel,Long> channels;
    
    public enum DevChannel {
        LOG,
        COUNT,
        LEVEL
        ;

        public static @Nullable DevChannel of(@NotNull final String value) {
            try {
                return DevChannel.valueOf(value);
            } catch (Exception ignored) {
                return null;
            }
        }

        public long getChannelId(final long serverId) {
            return Channels.get(serverId).getChannelId(this);
        }
    }

    private Channels(final long serverId) {
        if (instances.containsKey(serverId)) {
            throw new IllegalStateException("Channels instance already exists for server %d!".formatted(serverId));
        }

        this.id = serverId;
        channels = Jarvis.DATABASE.getChannelIds(this.id);

        synchronized (instances) {
            instances.put(this.id,this);
        }
    }

    public boolean save() {
        return Jarvis.DATABASE.saveChannelIds(this.id,channels);
    }

    public static boolean update(final long serverId, @NotNull final EnumMap<DevChannel,Long> updates) {
        return get(serverId).update(updates);
    }

    public boolean update(@NotNull final EnumMap<DevChannel,Long> updates) {
        channels.putAll(updates);
        return save();
    }

    public static boolean update(final long serverId, @NotNull final DevChannel channel, final long id) {
        return get(serverId).update(channel,id);
    }

    public boolean update(@NotNull final DevChannel channel, final long id) {
        channels.put(channel,id);
        return Jarvis.DATABASE.updateChannelId(this.id,channel,id);
    }

    @Contract(pure = true)
    public static long getChannelId(final long serverId, @NotNull final DevChannel channel) {
        return Channels.get(serverId).getChannelId(channel);
    }

    @Contract(pure = true)
    public long getChannelId(@NotNull final DevChannel channel) {
        return channels.getOrDefault(channel,0L);
    }

    public static @Nullable TextChannel getChannel(final long serverId, @NotNull final DevChannel channel) {
        return get(serverId).getChannel(channel);
    }

    public @Nullable TextChannel getChannel(@NotNull final DevChannel channel) {
        return getChannel(getChannelId(channel));
    }

    public static @Nullable TextChannel getChannel(final long id) {
        return Jarvis.jda().getTextChannelById(id);
    }

    public static @NotNull Channels get(final long serverId) {
        if (instances.containsKey(serverId)) return instances.get(serverId);
        else return new Channels(serverId);
    }
}
