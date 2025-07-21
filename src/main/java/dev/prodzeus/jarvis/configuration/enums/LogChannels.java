package dev.prodzeus.jarvis.configuration.enums;

public enum LogChannels {
    LOG("1379145039242068199"),
    USER("1380559321498390599"),
    AI("1381224971736580197")
    ;

    public final String id;

    LogChannels(final String id) {
        this.id = id;
    }
}
