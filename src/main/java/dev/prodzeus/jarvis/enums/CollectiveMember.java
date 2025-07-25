package dev.prodzeus.jarvis.enums;

public record CollectiveMember(long id, long server) {
    public String getMention() {
        return "<@%d>".formatted(id);
    }
}
