package dev.prodzeus.jarvis.configuration.enums;

public enum LogChannels {
    LOG(1379145039242068199L),
    USER(1380559321498390599L),
    AI(1381224971736580197L),
    DATABASE(1397204262202638416L)
    ;

    public final long id;

    LogChannels(final long id) {
        this.id = id;
    }
}
