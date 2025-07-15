package dev.prodzeus.tdcdb.commands;

import dev.prodzeus.tdcdb.bot.Bot;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Arrays;

public class CommandListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        Arrays.stream(Bot.INSTANCE.commandManager.getCommands()).forEach(cmd -> {
            if (e.getName().equals(cmd.name)) {
                cmd.handle(e);
            }
        });
    }
}
