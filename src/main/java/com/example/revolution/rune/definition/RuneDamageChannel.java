package com.example.revolution.rune.definition;

/** Canonical server-authoritative damage/status channels used by rune effects. */
public enum RuneDamageChannel {
    CLICK,
    POISON,
    BURNING,
    BLEEDING,
    CURSE,
    WITHER,
    DECAY;

    public static RuneDamageChannel fromDamageStat(RuneStatType statType) {
        if (statType == null) return null;
        switch (statType) {
            case CLICK_DAMAGE_FLAT:
            case CLICK_DAMAGE_MULTIPLIER:
                return CLICK;
            case POISON_DAMAGE_FLAT:
            case POISON_DAMAGE_MULTIPLIER:
                return POISON;
            case BURNING_DAMAGE_FLAT:
            case BURNING_DAMAGE_MULTIPLIER:
                return BURNING;
            case BLEEDING_DAMAGE_FLAT:
            case BLEEDING_DAMAGE_MULTIPLIER:
                return BLEEDING;
            case CURSE_DAMAGE_FLAT:
            case CURSE_DAMAGE_MULTIPLIER:
                return CURSE;
            case WITHER_DAMAGE_FLAT:
            case WITHER_DAMAGE_MULTIPLIER:
                return WITHER;
            case DECAY_DAMAGE_FLAT:
            case DECAY_DAMAGE_MULTIPLIER:
                return DECAY;
            default:
                return null;
        }
    }

    public static RuneDamageChannel fromVulnerabilityStat(RuneStatType statType) {
        if (statType == null) return null;
        switch (statType) {
            case CLICK_VULNERABILITY:
                return CLICK;
            case POISON_VULNERABILITY:
                return POISON;
            case BURNING_VULNERABILITY:
                return BURNING;
            case BLEEDING_VULNERABILITY:
                return BLEEDING;
            case CURSE_VULNERABILITY:
                return CURSE;
            case WITHER_VULNERABILITY:
                return WITHER;
            case DECAY_VULNERABILITY:
                return DECAY;
            default:
                return null;
        }
    }
}
