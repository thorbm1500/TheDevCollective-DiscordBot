package dev.prodzeus.jarvis.configuration.enums;

import dev.prodzeus.jarvis.bot.Bot;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.Nullable;

public enum LevelRoles {
    LEVEL_1(1,1379502338737176649L),
    LEVEL_5(5,1379502409159675986L),
    LEVEL_10(10,1379502662353027175L),
    LEVEL_15(15,1379502694032740372L),
    LEVEL_20(20,1379502731777015939L),
    LEVEL_25(25,1379502757492293682L),
    LEVEL_30(30,1379502788714823813L),
    LEVEL_35(35,1379502853466492991L),
    LEVEL_40(40,1379502910005575713L),
    LEVEL_45(45,1379502940150304788L),
    LEVEL_50(50,1379502962250088530L),
    LEVEL_55(55,1379503023080083537L),
    LEVEL_60(60,1379503048170274896L),
    LEVEL_65(65,1379503071461380217L),
    LEVEL_70(70,1379503092739084308L),
    LEVEL_75(75,1379503150964281494L),
    LEVEL_80(80,1379503184011329596L),
    LEVEL_85(85,1379503206601855118L),
    LEVEL_90(90,1379503223974531084L),
    LEVEL_95(95,1379503236977000590L),
    LEVEL_100(100,1379503254551007282L)
    ;

    public final int level;
    public final long id;

    LevelRoles(final int level, final long id) {
        this.level = level;
        this.id = id;
    }

    public static long getLevelId(final int level) {
        for (LevelRoles levelRole : LevelRoles.values()) if (level == levelRole.level) return levelRole.id;
        return LEVEL_1.id;
    }

    @Nullable
    public Role getRole() {
        return Bot.INSTANCE.jda.getRoleById(id);
    }

    @Nullable
    public static Role getRole(final int level) {
        for (LevelRoles levelRole : LevelRoles.values()) if (level == levelRole.level) return Bot.INSTANCE.jda.getRoleById(levelRole.id);
        return LEVEL_1.getRole();
    }
}
