package dev.prodzeus.jarvis.games.count;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@SuppressWarnings("unused")
public enum CountLevel {
    LEVEL_0(0,0,"count_level_0"),
    LEVEL_1(1,25,"count_level_1"),
    LEVEL_2(2,100,"count_level_2"),
    LEVEL_3(3,250,"count_level_3"),
    LEVEL_4(4,500,"count_level_4"),
    LEVEL_5(5,1000,"count_level_5"),
    LEVEL_6(6,2500,"count_level_6"),
    LEVEL_7(7,5000,"count_level_7"),
    LEVEL_8(8,7500,"count_level_8"),
    LEVEL_9(9,10000,"count_level_9")
    ;

    public final long level;
    public final long requirement;
    public final String emoji;

    CountLevel(final long level, final long requirement, @NotNull final String emoji) {
        this.level = level;
        this.requirement = requirement;
        this.emoji = emoji;
    }

    public static CountLevel of(final long level) {
        for(final CountLevel lvl : Arrays.stream(values()).toList().reversed()) {
            if (lvl.level == level) return lvl;
        }
        return level > 9 ? LEVEL_9 : LEVEL_0;
    }

    public static CountLevel ofRequirement(final long requirement) {
        for(final CountLevel lvl : Arrays.stream(values()).toList().reversed()) {
            if (lvl.requirement == requirement) return lvl;
        }
        return requirement > LEVEL_9.requirement ? LEVEL_9 : LEVEL_0;
    }

    public static CountLevel getCountLevel(final long counts) {
        for(final CountLevel lvl : Arrays.stream(values()).toList().reversed()) {
            if (lvl.requirement <= counts) return lvl;
        }
        return CountLevel.LEVEL_0;
    }

    public static long getNextLevelRequirement(final long level) {
        return of(level+1).requirement;
    }
}
