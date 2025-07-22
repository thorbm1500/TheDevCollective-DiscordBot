package dev.prodzeus.jarvis.enums;

public record Member(long id, long server) {
    public String getMention() {
        return "<@%d>".formatted(id);
    }
}
