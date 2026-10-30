package com.example.revolution.rune.definition;

/** Generic effect primitives. Concrete runes provide targets and values through definitions. */
public enum RuneEffectType {
    DAMAGE_FLAT,
    DAMAGE_MULTIPLIER,
    ALL_DAMAGE_MULTIPLIER,
    VULNERABILITY,
    EFFECTIVENESS,
    TARGETED_POWER,
    PROC_CHANCE,
    PROC_DAMAGE_MULTIPLIER,
    MONSTER_GOLD_MULTIPLIER,
    MONSTER_RESOURCE_MULTIPLIER,
    PET_XP_MULTIPLIER,
    DELAYED_DAMAGE_BONUS,
    TEMPORARY_DAMAGE_MULTIPLIER,
    TEMPORARY_EFFECT_DURATION,
    ENHANCEMENT_EFFECTIVENESS
}
