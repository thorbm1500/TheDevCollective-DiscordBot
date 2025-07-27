package dev.prodzeus.jarvis.commands;

import dev.prodzeus.jarvis.configuration.Channels;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

import static dev.prodzeus.jarvis.bot.Jarvis.BOT;
import static dev.prodzeus.jarvis.bot.Jarvis.LOGGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class CommandHandler extends ListenerAdapter {

    public CommandHandler() {
        final Collection<Command> commands = BOT.jda.retrieveCommands().complete();
        if (commands.isEmpty()) {
            LOGGER.info("No commands found. Registering commands...");
            registerCommands();
        } else {
            final Collection<CommandData> cache = new HashSet<>();
            for (final CollectiveCommand command : CollectiveCommand.values()) {
                if (commands.stream().noneMatch(cmd -> cmd.getName().equals(command.name))) {
                    cache.add(command.data);
                }
            }
            if (cache.isEmpty()) {
                LOGGER.debug("All commands successfully validated.");
            } else {
                LOGGER.info("One or more commands found to be missing from Discord. Updating commands...");
                BOT.jda.updateCommands().addCommands(cache).queue(
                        s -> LOGGER.info("All commands successfully updated and validated."),
                        f -> LOGGER.error("Failed to update and validate commands! {}", f)
                );
            }
        }
    }

    @Override
    public synchronized void onSlashCommandInteraction(@NotNull final SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.isAcknowledged()) return;
        final long serverId = event.getGuild().getIdLong();
        final Channels channels = Channels.get(serverId);

        final String commandName = event.getName();

        if ("restart-game".equals(commandName)) {
            if (event.getChannel().getIdLong() != channels.countChannel) return;
            //TODO: Add restarting for Count Game.
        }

        else if ("register".equals(commandName)) {
            switch (event.getSubcommandName()) {
                case "channel" -> {
                    Channels.get(serverId).update(
                            event.getOption("Channel Type").getAsString(),
                            event.getOption("Channel Instance").getAsChannel().getIdLong());
                }
                case "role" -> {

                }
                case "staff-role" -> {

                }
                case "level-role" -> {

                }
                case "level-roles" -> {

                }
            }
        }
    }

    private void registerCommands() {
        BOT.jda.updateCommands().addCommands(CollectiveCommand.getCommands()).queue(
                s -> LOGGER.info("Commands successfully registered!"),
                f -> LOGGER.error("Failed to register commands! {}",f)
        );
    }

    public enum CollectiveCommand {
        REGISTER(Commands
                .slash("register", "Register various components for the server.")
                .addSubcommands(
                        new SubcommandData("channel", "Register a channel.")
                                .addOptions(
                                        new OptionData(STRING,
                                                "Channel Type",
                                                "Select the type of channel to register.",
                                                true,
                                                true)
                                                .addChoices(new Command.Choice("Log Channel", "LOG"),
                                                        new Command.Choice("Count Channel", "COUNT"),
                                                        new Command.Choice("Levels Channel", "LEVEL")),
                                        new OptionData(CHANNEL,
                                                "Channel Instance",
                                                "Select the channel instance to register.",
                                                true,
                                                true)
                                                .setChannelTypes(ChannelType.TEXT)
                                ),
                        new SubcommandData("role", "Register a role.")
                                .addOptions(
                                        new OptionData(STRING,
                                                "Role Type",
                                                "Select the type of role to register.",
                                                true,
                                                true)
                                                .addChoices(new Command.Choice("Member", "MEMBER")),
                                        new OptionData(ROLE,
                                                "Role Instance",
                                                "Select the role instance to register.",
                                                true,
                                                false)
                                ),
                        new SubcommandData("staff-role", "Register a Staff role.")
                                .addOptions(
                                        new OptionData(STRING,
                                                "Role Type",
                                                "Select the type of role to register.",
                                                true,
                                                true)
                                                .addChoices(new Command.Choice("Member", "MEMBER")),
                                        new OptionData(ROLE,
                                                "Role Instance",
                                                "Select the role instance to register.",
                                                true,
                                                false)
                                ),
                        new SubcommandData("level-role", "Register a Levels role.")
                                .addOptions(
                                        new OptionData(STRING,
                                                "Role Level",
                                                "Select the level to register a role for.",
                                                true,
                                                true)
                                                .addChoices(
                                                        new Command.Choice("Level 1", 1),
                                                        new Command.Choice("Level 5", 5),
                                                        new Command.Choice("Level 10", 10),
                                                        new Command.Choice("Level 15", 15),
                                                        new Command.Choice("Level 20", 20),
                                                        new Command.Choice("Level 25", 25),
                                                        new Command.Choice("Level 30", 30),
                                                        new Command.Choice("Level 35", 35),
                                                        new Command.Choice("Level 40", 40),
                                                        new Command.Choice("Level 45", 45),
                                                        new Command.Choice("Level 50", 50),
                                                        new Command.Choice("Level 55", 55),
                                                        new Command.Choice("Level 60", 60),
                                                        new Command.Choice("Level 65", 65),
                                                        new Command.Choice("Level 70", 70),
                                                        new Command.Choice("Level 75", 75),
                                                        new Command.Choice("Level 80", 80),
                                                        new Command.Choice("Level 85", 85),
                                                        new Command.Choice("Level 90", 90),
                                                        new Command.Choice("Level 95", 95),
                                                        new Command.Choice("Level 100", 100)),
                                        new OptionData(ROLE,
                                                "Role Instance",
                                                "Select the role instance to register.",
                                                true,
                                                true)
                                ),
                        new SubcommandData("level-roles", "Register various level roles.")
                                .addOption(ROLE, "Level 1", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 5", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 10", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 15", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 20", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 25", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 30", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 35", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 40", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 45", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 50", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 55", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 60", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 65", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 70", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 75", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 80", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 85", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 90", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 95", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "Level 100", "Select the role instance to register.",
                                        false, false)
                )),
        RESTART_GAME(Commands.slash("restart-game",
                        "Restart the game in the channel the command is called in." +
                        "This command will only have an effect if there is an active game in the channel it's called from.")
                .addOption(NUMBER,
                        "Value",
                        "Pass a value to the game. The value will be ignored if the game does not take values on restart.",
                        false, false));

        public final String name;
        private final SlashCommandData data;

        CollectiveCommand(@NotNull final SlashCommandData data) {
            this.data = data;
            this.name = data.getName();
        }

        @NotNull
        public SlashCommandData command() {
            return data;
        }

        @NotNull
        public static Collection<CommandData> getCommands() {
            final Collection<CommandData> data = new HashSet<>();
            for (final CollectiveCommand command : values()) {
                data.add(command.command()
                        .setDefaultPermissions(DefaultMemberPermissions
                                .enabledFor(Permission.ADMINISTRATOR))
                );
            }
            return data;
        }
    }
}
