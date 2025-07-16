package dev.prodzeus.tdcdb.misc;

import dev.prodzeus.tdcdb.bot.Configuration;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MemberWelcome extends ListenerAdapter {
    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent e) {
        e.getGuild().getTextChannelById(Configuration.getWelcomeChannel().get()).sendMessage("NEW MEMBER EMOJI" + "** Welcome to the server " + e.getMember().getAsMention() + "!**").queue();
        e.getGuild().addRoleToMember(UserSnowflake.fromId(e.getMember().getId()), e.getGuild().getRoleById(Configuration.getMemberRole().get())).queue();
        // TODO ADD USER TO DB
    }
}
