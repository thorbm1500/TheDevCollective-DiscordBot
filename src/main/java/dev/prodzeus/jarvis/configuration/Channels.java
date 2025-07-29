package dev.prodzeus.jarvis.configuration;

import dev.prodzeus.jarvis.bot.Jarvis;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Channels {

    private static final Map<Long, Channels> instances = new HashMap<>();

    private final long id;

    public long logChannel = 0L;
    public long countChannel = 0L;
    public long levelChannel = 0L;

    private Channels(final long serverId) {
        if (instances.containsKey(serverId)) {
            throw new IllegalStateException("Channels instance already exists for server %d!".formatted(serverId));
        }
        this.id = serverId;
        final ChannelIds ids = Jarvis.DATABASE.getChannelIds(serverId);
        if (ids.log != null) this.logChannel = ids.log;
        if (ids.count != null) this.countChannel = ids.count;
        if (ids.level != null) this.levelChannel = ids.level;
        instances.put(this.id,this);
    }

    public void update(@NotNull final String channel, final long id) {
        switch (channel.toUpperCase()) {
            case "LOG" -> {
                logChannel = id;
                Jarvis.DATABASE.saveChannelIds(this.id,new ChannelIds(id,null,null));
            }
            case "COUNT" -> {
                countChannel = id;
                Jarvis.DATABASE.saveChannelIds(this.id,new ChannelIds(null,id,null));
            }
            case "LEVEL" -> {
                levelChannel = id;
                Jarvis.DATABASE.saveChannelIds(this.id,new ChannelIds(null,null,id));
            }
            default -> Jarvis.LOGGER.warn("Unknown channel type. Cannot update channel id!");
        }
    }

    public TextChannel getChannel(final long id) {
        return Jarvis.jda().getTextChannelById(id);
    }

    public static Channels get(final long serverId) {
        if (instances.containsKey(serverId)) return instances.get(serverId);
        else return instances.put(serverId, new Channels(serverId));
    }

    public record ChannelIds(Long log, Long count, Long level) {}
}
