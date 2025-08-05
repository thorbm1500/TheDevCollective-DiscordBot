package dev.prodzeus.jarvis.games.components;

import dev.prodzeus.jarvis.configuration.Channels;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public abstract class GameData  {

    public final long serverId;
    public final TextChannel channel;
    public final long channelId;

    protected GameData(final long serverId, final long channelId) {
        this.serverId = serverId;
        this.channel = Channels.getChannel(channelId);
        this.channelId = channelId;
    }

    public void saveAndReset() {
        save();
        reset();
    }

    protected abstract void save();
    protected abstract void reset();
}
