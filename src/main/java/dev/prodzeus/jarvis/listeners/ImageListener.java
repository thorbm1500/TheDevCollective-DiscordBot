package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.configuration.Channels;
import dev.prodzeus.jarvis.member.CollectiveMember;
import dev.prodzeus.jarvis.member.MemberManager;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static dev.prodzeus.jarvis.utility.Util.isValidMessageEvent;

public class ImageListener extends ListenerAdapter {

    private static final Set<String> imageTypes = Set.of("jpg","png","jpeg","webp","gif");

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (!isValidMessageEvent(e)) return;
        final long serverId = e.getGuild().getIdLong();
        if (e.getChannel().getIdLong() != Channels.getChannelId(serverId, Channels.DevChannel.MEMES)) return;
        if (e.getMessage().getAttachments().stream().noneMatch(attachment -> attachment.getFileExtension() != null && imageTypes.contains(attachment.getFileExtension()))) return;
        MemberManager.getCollectiveMember(serverId,e.getAuthor().getIdLong()).increment(CollectiveMember.MemberData.IMAGES_SENT);
    }

}
