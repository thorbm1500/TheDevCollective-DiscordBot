package dev.prodzeus.tdcdb.configuration;

import dev.prodzeus.tdcdb.bot.Bot;
import dev.prodzeus.tdcdb.bot.Main;
import me.ixwavey.utilities.io.YamlConfiguration;

import java.util.HashSet;
import java.util.Set;

public abstract class Configuration extends YamlConfiguration {

    private static final Set<Configuration> instances = new HashSet<>();
    protected final String path;

    protected Configuration(final String path) {
        super("configuration.yml");
        this.path = path.endsWith(".") ? path : path + ".";
        instances.add(this);
    }

    protected abstract void load();
    protected abstract void reload();

    public static void loadConfiguration() {
        instances.forEach(Configuration::load);
    }

    public static void reloadConfiguration() {
        instances.forEach(Configuration::reload);
    }
}