package dev.prodzeus.jarvis.commands;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.configuration.Channels;
import dev.prodzeus.jarvis.configuration.Roles;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class CommandHandler extends ListenerAdapter {

    private static final Logger LOGGER = SLF4JProvider.get().getLogger("Commands");

    public CommandHandler() {
        if (System.getenv().getOrDefault("UPDATE_COMMANDS","FALSE").equalsIgnoreCase("TRUE")) registerCommands();
    }

    @Override
    public synchronized void onSlashCommandInteraction(@NotNull final SlashCommandInteractionEvent event) {
        if (event.isFromGuild() && !event.isAcknowledged()) {
            if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) sendAck(event);
            else {
                LOGGER.trace("[Member:{}] Access denied!", event.getMember().getIdLong());
                event.getInteraction().reply(Jarvis.getEmojiFormatted("circle_failed") + " **Permission denied!**").setEphemeral(true).queue();
                return;
            }
        }
        else {
            LOGGER.error("[Server:{}] Failed to handle command!",event.getGuild().getIdLong());
            event.reply("Error occurred during command completion!").setEphemeral(true).setSuppressedNotifications(true).queue();
            return;
        }
        final long serverId = event.getGuild().getIdLong();

        final String commandName = event.getName();
        LOGGER.debug("Incoming command: {}",commandName);

        if ("restart-game".equals(commandName)) {
            if (event.getChannel().getIdLong() != Channels.DevChannel.COUNT.getChannelId(serverId)) {
                completeInteraction(event, "No active game found.");
                return;
            }
            completeInteraction(event, "Feature yet to be added.");
            //TODO: Add restarting for Count Game.
        } else if ("register".equals(commandName)) {
            final String subCommandName = event.getSubcommandName();
            switch (subCommandName) {
                case "channel" -> {
                    try {
                        final EnumMap<Channels.DevChannel,Long> updates = new EnumMap<>(Channels.DevChannel.class);
                        for (final OptionMapping option : event.getOptions()) {
                            final Channels.DevChannel channel = Channels.DevChannel.of(option.getName().toUpperCase());
                            updates.put(channel,option.getAsLong());
                        }
                        if (Channels.update(serverId,updates)) {
                            completeInteraction(event, "Channels updated!");
                            return;
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to update channels! {}",e);
                    }
                    completeInteraction(event, "Failed to update channels...");
                }
                case "role" -> {
                    try {
                        final EnumMap<Roles.DevRole,Long> updates = new EnumMap<>(Roles.DevRole.class);
                        for (final OptionMapping option : event.getOptions()) {
                            final Roles.DevRole role = Roles.DevRole.of(option.getName().toUpperCase());
                            updates.put(role,option.getAsLong());
                        }
                        if (Roles.update(serverId,updates)) {
                            completeInteraction(event, "Roles updated!");
                            return;
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to update roles! {}",e);
                    }
                    completeInteraction(event, "Failed to update roles...");
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
        Jarvis.jda().updateCommands().addCommands(CollectiveCommand.getCommands()).queue(
                s -> LOGGER.info("Commands successfully registered!"),
                f -> LOGGER.error("Failed to register commands! {}", f)
        );
    }

    public enum CollectiveCommand {
        REGISTER(Commands
                .slash("register", "Register/update various components for the server.")
                .addSubcommands(
                        new SubcommandData("channel", "Register/Update a channel.")
                                .addOptions(getChannelOptions()),
                        new SubcommandData("role", "Register/Update a role.")
                                .addOptions(getRoleOptions()))),
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
            try {
                for (final CollectiveCommand command : values()) {
                    data.add(command.command()
                            .setDefaultPermissions(DefaultMemberPermissions
                                    .enabledFor(Permission.ADMINISTRATOR))
                    );
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return data;
        }

        private static Collection<OptionData> getChannelOptions() {
            final HashSet<OptionData> data = HashSet.newHashSet(3);
            for (final Channels.DevChannel channel : Channels.DevChannel.values()) {
                data.add(new OptionData(CHANNEL,channel.toString().toLowerCase(),"Update channel ID.",false,false)
                        .setChannelTypes(ChannelType.TEXT));
            }
            return data;
        }

        private static Collection<OptionData> getRoleOptions() {
            final HashSet<OptionData> data = HashSet.newHashSet(24);
            for (final Roles.DevRole channel : Roles.DevRole.values()) {
                data.add(new OptionData(ROLE,channel.toString().toLowerCase(),"Update role ID.",false,false));
            }
            return data;
        }
    }
}
