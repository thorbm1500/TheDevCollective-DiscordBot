package dev.prodzeus.jarvis.configuration.enums;

public enum LevelRoles {
    LEVEL_1(1,"1379502338737176649"),
    LEVEL_5(5,"1379502409159675986"),
    LEVEL_10(10,"1379502662353027175"),
    LEVEL_15(15,"1379502694032740372"),
    LEVEL_20(20,"1379502731777015939"),
    LEVEL_25(25,"1379502757492293682"),
    LEVEL_30(30,"1379502788714823813"),
    LEVEL_35(35,"1379502853466492991"),
    LEVEL_40(40,"1379502910005575713"),
    LEVEL_45(45,"1379502940150304788"),
    LEVEL_50(50,"1379502962250088530"),
    LEVEL_55(55,"1379503023080083537"),
    LEVEL_60(60,"1379503048170274896"),
    LEVEL_65(65,"1379503071461380217"),
    LEVEL_70(70,"1379503092739084308"),
    LEVEL_75(75,"1379503150964281494"),
    LEVEL_80(80,"1379503184011329596"),
    LEVEL_85(85,"1379503206601855118"),
    LEVEL_90(90,"1379503223974531084"),
    LEVEL_95(95,"1379503236977000590"),
    LEVEL_100(100,"1379503254551007282")
    ;

    public final String id;
    public final int level;

    LevelRoles(final int level, final String id) {
        this.level = level;
        this.id = id;
    }

    public static String getLevelId(final int level) {
        for (LevelRoles levelRole : LevelRoles.values()) if (level == levelRole.level) return levelRole.id;
        return LEVEL_1.id;
    }
}
