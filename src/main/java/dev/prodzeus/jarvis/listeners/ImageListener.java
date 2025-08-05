package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.configuration.Channels;
import dev.prodzeus.jarvis.member.CollectiveMember;
import dev.prodzeus.jarvis.member.MemberManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ImageListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        final User user = e.getAuthor();
        if (user.isBot() || user.isSystem() || e.isWebhookMessage()) return;
        final long serverId = e.getGuild().getIdLong();
        if ( e.getChannel().getIdLong() != Channels.getChannelId(serverId, Channels.DevChannel.MEMES)) return;
        MemberManager.getCollectiveMember(serverId,user.getIdLong()).increment(CollectiveMember.MemberData.IMAGES_SENT);
    }

}
