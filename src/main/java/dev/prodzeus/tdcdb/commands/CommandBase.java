package dev.prodzeus.tdcdb.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public abstract class CommandBase {
    public String name;
    public String description;

    public SlashCommandData initialize() {
        var cmd = Commands.slash(name, description);
        configure(cmd);
        return cmd;
    }

    public abstract void configure(SlashCommandData command);

    public abstract void handle(SlashCommandInteractionEvent e);
}
