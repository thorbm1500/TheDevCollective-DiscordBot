package dev.prodzeus.jarvis.listeners;

import dev.prodzeus.jarvis.bot.Bot;
import dev.prodzeus.jarvis.configuration.enums.Channel;
import dev.prodzeus.jarvis.configuration.enums.Roles;
import dev.prodzeus.jarvis.enums.Emoji;
import dev.prodzeus.jarvis.utils.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class MemberWelcome extends ListenerAdapter {

    private final TextChannel channel = Channel.WELCOME.getChannel();
    private long memberCount = Bot.database.getMemberCount(Utils.getGuild().getIdLong());

    @Override
    public void onGuildMemberJoin(@NotNull final GuildMemberJoinEvent e) {
        final Member member = e.getMember();
        if (Bot.database.addMember(Utils.getMember(member))) {
            channel.sendMessage(String.format(Locale.GERMAN,"**%s Welcome to the server %s!**\n-# Member: #%,d",Emoji.WAVE.formatted, member.getAsMention(),++memberCount))
                    .queue(null, f -> Bot.INSTANCE.logger.warn("Failed to send welcome message for user {}! {}", member.getAsMention(), f));
        } else Bot.INSTANCE.logger.warn("Member {} was not added to the database and a welcome message has not been sent! If they're already in the database, then you can look past this warning.", member.getAsMention());
        Roles.MEMBER.addRole(member);
    }
}