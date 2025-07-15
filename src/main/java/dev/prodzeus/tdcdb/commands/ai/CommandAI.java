package dev.prodzeus.tdcdb.commands.ai;

import dev.prodzeus.tdcdb.commands.ACommand;
import dev.prodzeus.tdcdb.commands.CommandBase;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

@ACommand(value = "ai", description = "Prompt the AI.")
public class CommandAI extends CommandBase {
    @Override
    public void configure(SlashCommandData command) {
        command.addOption(OptionType.STRING, "prompt", "The prompt for the AI.", true);
    }

    @Override
    public void handle(SlashCommandInteractionEvent e) {
        e.reply("NOPE").setEphemeral(true).queue();
    }
}
