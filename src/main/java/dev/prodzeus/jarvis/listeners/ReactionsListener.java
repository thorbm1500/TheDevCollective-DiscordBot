package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.member.MemberManager;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ReactionsListener extends ListenerAdapter {

    private static final Logger LOGGER = SLF4JProvider.get().getLoggerFactory().getLogger("Reactions");

    public ReactionsListener() {}

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        User user = event.getUser();
        if (user == null) user = event.getJDA().retrieveUserById(event.getUserIdLong()).complete();
        if (user.isBot() || user.isSystem()) return;
        final long messageId = event.getMessageIdLong();
        final long guildId = event.getGuild().getIdLong();
        MemberManager.getCollectiveMember(guildId, user.getIdLong()).handleGiveReaction(messageId);
        MemberManager.getCollectiveMember(guildId, event.getMessageAuthorIdLong()).handleReceivedReaction(messageId);
        LOGGER.trace("{} reacted to {}'s message.",user.getIdLong(), event.getMessageAuthorIdLong());
    }
}
