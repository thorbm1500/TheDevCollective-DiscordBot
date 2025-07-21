package dev.prodzeus.jarvis.configuration.enums;

public enum Channels {
    WELCOME("1379132249856807052"),
    COUNT("1379134564340863086"),
    LEVEL("1379134479402143834"),
    COMMANDS("1379134509978488873"),
    AI("1381246280600387686")
    ;

    public final String id;

    Channels(final String id) {
        this.id = id;
    }
}
