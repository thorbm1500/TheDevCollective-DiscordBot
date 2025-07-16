package dev.prodzeus.tdcdb.bot;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.cli.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class Configuration {
    @Getter
    private static Opt token = new Opt("t", "token");
    @Getter
    private static Opt countChannel = new Opt("cid", "countId", "1379134564340863086");
    @Getter
    private static Opt ticketsCategory = new Opt("tid", "ticketsId", "1383188711373537290");
    @Getter
    private static Opt geminiToken = new Opt("ait", "aiToken");
    @Getter
    private static Opt aiChannel = new Opt("aic", "aiChannel", "1381246280600387686");
    @Getter
    private static Opt welcomeChannel = new Opt("w", "welcomeId", "1379132249856807052");
    @Getter
    private static Opt memberRole = new Opt("m", "memberId", "1379135561989750886");

    public static void initialize(String[] args) {
        var opts = new Options();

        Arrays.stream(Configuration.class.getDeclaredFields()).forEach(f -> {
            try {
                var opt = ((Opt) f.get(null));
                var option = new Option(opt.getShortName(), opt.getLongName(), true, "");
                option.setRequired(opt.getDefaultValue().isEmpty());
                opts.addOption(option);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });

        var parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(opts, args);
        } catch (ParseException e) {
            new HelpFormatter().printHelp("tdcdb", opts);
            throw new RuntimeException("failed to parse cli");
        }

        Arrays.stream(Configuration.class.getDeclaredFields()).forEach(f -> {
            try {
                var opt = (Opt) f.get(null);
                var result = cmd.getOptionValue(opt.getLongName());
                if (opt.getDefaultValue().isEmpty()) {
                    opt.setValue(result);
                } else {
                    opt.setValue(Objects.requireNonNullElse(result, opt.getDefaultValue().get()));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Getter
    @RequiredArgsConstructor
    public static class Opt {
        @NonNull
        private String shortName;
        @NonNull
        private String longName;
        private Optional<String> defaultValue = Optional.empty();
        @Setter
        private String value = null;

        public Opt(String shortName, String longName, String defaultValue) {
            this(shortName, longName);
            this.defaultValue = Optional.of(defaultValue);
        }

        public String get() {
            return value;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface AOption {
    }
}
