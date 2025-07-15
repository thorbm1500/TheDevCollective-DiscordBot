package dev.prodzeus.tdcdb.enums;

@SuppressWarnings("unused")
public enum Emoji {
    NITRO_1_MONTH("<:nitro_1_month:1394817034113781870>"),
    NITRO_2_MONTHS("<:nitro_2_months:1394817172789788772>"),
    NITRO_3_MONTHS("<:nitro_3_months:1394817290306064494>"),
    NITRO_6_MONTHS("<:nitro_6_months:1394817490080764067>"),
    NITRO_9_MONTHS("<:nitro_9_months:1394817621064552530>"),
    NITRO_12_MONTHS("<:nitro_12_months:1394817724718256158>"),
    NITRO_15_MONTHS("<:nitro_15_months:1394817879546789969>"),
    NITRO_18_MONTHS("<:nitro_18_months:1394818021863719114>"),
    NITRO_24_MONTHS("<:nitro_24_months:1394818135739334717>"),
    BUG_HUNTER("<:bug_hunter:1394816116316045332>"),
    GOLDEN_BUG_HUNTER("<:golden_bug_hunter:1394818548169310291>"),
    EARLY_SUPPORTER("<:early_supporter:1394818251489411103>"),
    PARTNERED_SERVER_OWNER("<:partnered_server_owner:1394818351586476052>"),
    ORBS("<:orbs:1394818456053874771>"),
    COMPLETED_QUEST("<:completed_quest:1394818663357612105>"),
    SUBSCRIBER("<:subscriber:1394818747042369729>"),
    ACTIVE_DEVELOPER("<:active_developer:1394818832480338074>"),
    EARLY_VERIFIED_DEVELOPER("<:early_verified_developer:1394818928299081739>"),
    DISCORD_STAFF("<:discord_staff:1394819056963682546>"),
    CERTIFIED_MODERATOR("<:certified_moderator:1394819153147461752>"),
    MODERATOR_PROGRAMS("<:moderator_programs:1394819269174493194>"),
    ORIGINALLY_KNOWN_AS("<:originally_known_as:1394819383796433007>"),
    HYPESQUAD_BALANCE("<:hypesquad_balance:1394819488033275965>"),
    HYPESQUAD_BRAVERY("<:hypesquad_bravery:1394819548552757288>"),
    HYPESQUAD_EVENTS("<:hypesquad_bravery:1394819548552757288>"),
    HYPESQUAD_BRILLIANCE("<:hypesquad_brilliance:1394819648054231241>"),
    DOT_WHITE("<:dot_white:1394820964872618135>"),
    DOT_GRAY("<:dot_gray:1394820079429877770>"),
    DOT_PINK("<:dot_pink:1394820211747586131>"),
    DOT_RED("<:dot_red:1394820309160558632>"),
    DOT_ORANGE("<:dot_orange:1394820416463311089>"),
    DOT_YELLOW("<:dot_yellow:1394820528321200138>"),
    DOT_GREEN("<:dot_green:1394820624085553223>"),
    DOT_PURPLE("<:dot_purple:1394820715479306291>"),
    DOT_BLUE("<:dot_blue:1394820800783188130>"),
    DOT_CYAN("<:dot_cyan:1394820888356061184>"),
    YOUTUBE_LOGO("<:youtube:1394821153683669142>"),
    GITHUB_LOGO("<:github:1394821242611175527>"),
    TELEGRAM_LOGO_WHITE("<:telegram_white:1394821300136185886>"),
    PURPLE_MOD_SHIELD("<:purple_moderator_shield:1394821842790912010>"),
    MUTE("<:mute:1394822754896711720>"),
    DEAFEN("<a:deafen:1394822639486500948>"),
    CROWN("<:crown:1394822926569574490>"),
    VERIFIED_BLUE("<:verified_blue:1394823343039054035>"),
    VERIFIED_GREEN("<:verified_green:1394823965267988533>"),
    LIKE("<:like:1394824135233900646>"),
    DISLIKE("<:dislike:1394824156503343295>")
    ;

    public final String id;

    Emoji(final String id) {
        this.id = id;
    }
}