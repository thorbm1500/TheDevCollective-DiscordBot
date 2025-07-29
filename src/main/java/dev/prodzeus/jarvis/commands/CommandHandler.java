package dev.prodzeus.jarvis.commands;

import dev.prodzeus.jarvis.configuration.Channels;
import dev.prodzeus.jarvis.configuration.Roles;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
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
                if (commands.stream().noneMatch(cmd -> cmd.getName().equals(command.getName()))) {
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
        if (event.isFromGuild() && !event.isAcknowledged()) sendAck(event);
        else {
            event.reply("Error occurred during command completion!").setEphemeral(true).setSuppressedNotifications(true).queue();
            return;
        }
        final long serverId = event.getGuild().getIdLong();
        final Channels channels = Channels.get(serverId);

        final String commandName = event.getName();

        LOGGER.debug("New command call registered.\nCommand: {}\nSubCommand: {}\nOption: channel_type: {}\nOption: channel_instance: {}",
                commandName,event.getSubcommandName(),event.getOption("channel_type").getAsString(),event.getOption("channel_instance").getAsLong());

        if ("restart-game".equals(commandName)) {
            if (event.getChannel().getIdLong() != channels.countChannel) {
                completeInteraction(event, "No active game found.");
                return;
            }
            completeInteraction(event, "Feature yet to be added.");
            //TODO: Add restarting for Count Game.
        } else if ("register".equals(commandName)) {
            final String subCommandName = event.getSubcommandName();
            switch (subCommandName) {
                case "channel" -> {
                    Channels.get(serverId)
                            .update(event.getOption("channel").getAsString(),
                                    event.getOption("channel_instance").getAsLong());
                    completeInteraction(event, "Channel updated!");
                }
                case "role" -> {
                    Roles.get(serverId)
                            .update(event.getOption("role").getAsString(),
                                    event.getOption("role_instance").getAsLong());
                    completeInteraction(event, "Role updated!");
                }
                case "level-role" -> {
                    final Roles roles = Roles.get(serverId);
                    try {
                        for (final OptionMapping option : event.getOptions()) {
                            roles.update(option.getName(), option.getAsLong(), true);
                        }
                        roles.updateDatabase();
                        completeInteraction(event, "Roles updated!");
                    } catch (Exception e) {
                        completeInteraction(event, "Failed to update roles...");
                        LOGGER.error("Failed to register updates for level roles! {}",e);
                    }
                }
                default -> {
                    completeInteraction(event, "Unknown command! `/register %s`".formatted(subCommandName));
                    LOGGER.error("Unknown command! {}", subCommandName);
                }
            }
        }
    }

    private void sendAck(@NotNull final SlashCommandInteractionEvent event) {
        event.deferReply(true).setSuppressedNotifications(true).queue();
    }

    private void completeInteraction(@NotNull final SlashCommandInteractionEvent event, @NotNull final String message) {
        completeInteraction(event, message, true);
    }

    private void completeInteraction(@NotNull final SlashCommandInteractionEvent event, @NotNull final String message, final boolean ephemeral) {
        event.getHook().sendMessage(message).setEphemeral(ephemeral).queue();
    }

    private void registerCommands() {
        BOT.jda.updateCommands().addCommands(CollectiveCommand.getCommands()).queue(
                s -> LOGGER.info("Commands successfully registered!"),
                f -> LOGGER.error("Failed to register commands! {}", f)
        );
    }

    public enum CollectiveCommand {
        REGISTER(Commands
                .slash("register", "Register various components for the server.")
                .addSubcommands(
                        new SubcommandData("channel", "Register/Update a channel.")
                                .addOptions(
                                        new OptionData(STRING,
                                                "channel",
                                                "Select the type of channel to register.",
                                                true,
                                                false)
                                                .addChoices(new Command.Choice("Log Channel", "LOG"),
                                                        new Command.Choice("Count Channel", "COUNT"),
                                                        new Command.Choice("Levels Channel", "LEVEL")),
                                        new OptionData(CHANNEL,
                                                "channel_instance",
                                                "Select the channel instance to register.",
                                                true,
                                                false)
                                                .setChannelTypes(ChannelType.TEXT)
                                ),
                        new SubcommandData("role", "Register/Update a role.")
                                .addOptions(
                                        new OptionData(STRING,
                                                "role",
                                                "Select the type of role to register.",
                                                true,
                                                false)
                                                .addChoices(new Command.Choice("Member", "MEMBER"))
                                                .addChoices(new Command.Choice("Staff", "STAFF")),
                                        new OptionData(ROLE,
                                                "role_instance",
                                                "Select the role instance to register.",
                                                true,
                                                false)
                                ),
                        new SubcommandData("level-role", "Register/Update one or more level roles.")
                                .addOption(ROLE, "level_1", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_5", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_10", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_15", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_20", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_25", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_30", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_35", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_40", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_45", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_50", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_55", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_60", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_65", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_70", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_75", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_80", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_85", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_90", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_95", "Select the role instance to register.",
                                        false, false)
                                .addOption(ROLE, "level_100", "Select the role instance to register.",
                                        false, false)
                )),
        RESTART_GAME(Commands.slash("restart-game", "Restart the game in the channel the command is called in.")
                .addOption(NUMBER,
                        "value",
                        "Pass a value to the game. The value will be ignored if the game does not take values on restart.",
                        false, false));

        private final SlashCommandData data;

        CollectiveCommand(@NotNull final SlashCommandData data) {
            this.data = data;
        }

        public @NotNull String getName() {
            return data.getName();
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
