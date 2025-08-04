package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Jarvis;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class GuildListener extends ListenerAdapter {

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Jarvis.LOGGER.info("[Server:{}:{}] New server registered! Setting up server...", event.getGuild().getName(), event.getGuild().getIdLong());
        Jarvis.DATABASE.addServer(event.getGuild().getIdLong());
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        Jarvis.LOGGER.info("[Server:{}:{}] Server unregistered! Removing server...", event.getGuild().getName(), event.getGuild().getIdLong());
        Jarvis.DATABASE.removeServer(event.getGuild().getIdLong());
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Jarvis.DATABASE.addMember(event.getMember());
    }
}
