package dev.prodzeus.tdcdb;

import lombok.Getter;
import org.apache.commons.cli.*;

import java.util.Objects;

public class Configuration {
    @Getter
    private static String token;
    @Getter
    private static String countChannel;
    @Getter
    private static String ticketsCategory;

    public static void initialize(String[] args) {
        var opts = new Options();

        var token = new Option("t", "token", true, "");
        token.setRequired(true);
        opts.addOption(token);

        var count = new Option("cid", "countId", true, "");
        count.setRequired(false);
        opts.addOption(count);

        var tickets = new Option("tid", "ticketsId", true, "");
        tickets.setRequired(false);
        opts.addOption(tickets);

        var parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(opts, args);
        } catch (ParseException e) {
            new HelpFormatter().printHelp("tdcdb", opts);
            throw new RuntimeException("failed to parse cli");
        }

        Configuration.token = cmd.getOptionValue("token");
        Configuration.countChannel = Objects.requireNonNullElse(cmd.getOptionValue("countId"), "1379134564340863086");
        Configuration.ticketsCategory = Objects.requireNonNullElse(cmd.getOptionValue("ticketsId"), "1383188711373537290");
    }
}
