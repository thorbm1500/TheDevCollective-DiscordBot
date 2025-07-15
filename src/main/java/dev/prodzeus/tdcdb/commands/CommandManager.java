package dev.prodzeus.tdcdb.commands;

import dev.prodzeus.tdcdb.bot.Bot;
import lombok.Getter;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.util.Arrays;
import java.util.Objects;

public class CommandManager {
    @Getter
    private CommandBase[] commands;

    public CommandManager() {
        commands = new Reflections("dev.prodzeus.tdcdb.commands", Scanners.TypesAnnotated)
                .getTypesAnnotatedWith(ACommand.class)
                .stream()
                .map(commandClass -> {
                    try {
                        var cmd = (CommandBase) commandClass.getConstructor().newInstance();
                        var cmda = commandClass.getAnnotation(ACommand.class);
                        cmd.name = cmda.value();
                        cmd.description = cmda.description();
                        return cmd;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toArray(CommandBase[]::new);
    }

    public void setupCommands() {
        var cmds = Bot.INSTANCE.jda.updateCommands();

        Arrays.stream(commands).forEach(cmd -> cmds.addCommands(cmd.initialize()));
        cmds.queue();
    }
}
