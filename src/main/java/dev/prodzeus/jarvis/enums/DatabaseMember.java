package dev.prodzeus.jarvis.enums;

@SuppressWarnings("unused")
public record DatabaseMember(long id, long server, int level, long experience, int correctCounts, int incorrectCounts) {}
