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
    private static Opt geminiToken = new Opt("ait", "aiToken");
    @Getter
    private static Opt db = new Opt("db", "db");
    @Getter
    private static Opt dbu = new Opt("dbu", "dbUser");
    @Getter
    private static Opt dbp = new Opt("dbp", "dbPass");

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
