package dev.prodzeus.jarvis.misc;

import dev.prodzeus.jarvis.enums.Emoji;
import dev.prodzeus.jarvis.logger.Logger;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Ready extends ListenerAdapter {
    @Override
    public void onReady(@NotNull ReadyEvent e) {
        e.getJDA().getPresence().setPresence(OnlineStatus.ONLINE, Activity.watching("\uD835\uDD80\uD835\uDD7E\uD835\uDD70\uD835\uDD7D count"));
        Logger.info(Emoji.DOT_GREEN.id + "** Bot enabled as %s**\n-# Enabled: <t:%d:R>".formatted(e.getJDA().getSelfUser().getName(), (System.currentTimeMillis() / 1000)));
    }
}
