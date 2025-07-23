package dev.prodzeus.jarvis.listeners;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Ready extends ListenerAdapter {
    @Override
    public void onReady(@NotNull ReadyEvent e) {
        e.getJDA().getPresence().setPresence(OnlineStatus.ONLINE, Activity.watching("\uD835\uDD80\uD835\uDD7E\uD835\uDD70\uD835\uDD7D count"));
        e.getJDA().removeEventListener(this);
    }
}
