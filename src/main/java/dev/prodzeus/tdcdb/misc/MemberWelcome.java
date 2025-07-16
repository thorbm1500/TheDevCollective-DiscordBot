package dev.prodzeus.tdcdb.misc;

import dev.prodzeus.tdcdb.bot.Bot;
import dev.prodzeus.tdcdb.bot.Configuration;
import dev.prodzeus.tdcdb.utils.Utils;
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
        e.getGuild().getTextChannelById(Configuration.getWelcomeChannel().get()).sendMessage("NEW MEMBER EMOJI" + "** Welcome to the server " + e.getMember().getAsMention() + "!**").queue();
        e.getGuild().addRoleToMember(UserSnowflake.fromId(e.getMember().getId()), e.getGuild().getRoleById(Configuration.getMemberRole().get())).queue();
        try {
            addMember.setLong(1, e.getMember().getIdLong());
            addMember.executeUpdate();
        } catch (SQLException ex) {
            Bot.logger.error(Bot.dLog, "Failed to add user to db", ex);
            throw new RuntimeException(ex);
        }
    }
}
