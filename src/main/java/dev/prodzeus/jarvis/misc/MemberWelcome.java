package dev.prodzeus.jarvis.misc;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.configuration.enums.Channels;
import dev.prodzeus.jarvis.configuration.enums.Roles;
import dev.prodzeus.jarvis.logger.Logger;
import dev.prodzeus.jarvis.utils.Utils;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MemberWelcome extends ListenerAdapter {
    private PreparedStatement addMember = Utils.prepareStatement("INSERT INTO members (id) VALUES (?)");

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent e) {
        e.getGuild().getTextChannelById(Channels.WELCOME.id).sendMessage("NEW MEMBER EMOJI" + "** Welcome to the server " + e.getMember().getAsMention() + "!**").queue();
        e.getGuild().addRoleToMember(UserSnowflake.fromId(e.getMember().getId()), e.getGuild().getRoleById(Roles.MEMBER.id)).queue();
    }
}
