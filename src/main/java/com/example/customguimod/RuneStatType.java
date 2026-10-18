package com.example.customguimod;

/**
 * Stable identifiers for scalar, player-facing rune stat categories.
 *
 * Composite effects that need more than one value (for example proc chance +
 * proc damage, delayed damage, or temporary triggered damage) intentionally
 * live in RuneCompositeEffect instead of being forced into one double here.
 */
enum RuneStatType {
    CLICK_DAMAGE_FLAT("Урон Клика", DisplayKind.FLAT),
    CLICK_DAMAGE_MULTIPLIER("Урон Клика", DisplayKind.MULTIPLIER),
    POISON_DAMAGE_FLAT("Урон Яда", DisplayKind.FLAT),
    POISON_DAMAGE_MULTIPLIER("Урон Яда", DisplayKind.MULTIPLIER),
    BURNING_DAMAGE_FLAT("Урон Горения", DisplayKind.FLAT),
    BURNING_DAMAGE_MULTIPLIER("Урон Горения", DisplayKind.MULTIPLIER),
    BLEEDING_DAMAGE_FLAT("Урон Кровотечения", DisplayKind.FLAT),
    BLEEDING_DAMAGE_MULTIPLIER("Урон Кровотечения", DisplayKind.MULTIPLIER),
    CURSE_DAMAGE_FLAT("Урон Проклятия", DisplayKind.FLAT),
    CURSE_DAMAGE_MULTIPLIER("Урон Проклятия", DisplayKind.MULTIPLIER),
    WITHER_DAMAGE_FLAT("Урон Иссушения", DisplayKind.FLAT),
    WITHER_DAMAGE_MULTIPLIER("Урон Иссушения", DisplayKind.MULTIPLIER),
    DECAY_DAMAGE_FLAT("Урон Разложения", DisplayKind.FLAT),
    DECAY_DAMAGE_MULTIPLIER("Урон Разложения", DisplayKind.MULTIPLIER),

    CLICK_VULNERABILITY("Уязвимость к Урону Клика", DisplayKind.PERCENT),
    POISON_VULNERABILITY("Уязвимость к Урону Яда", DisplayKind.PERCENT),
    BURNING_VULNERABILITY("Уязвимость к Урону Горения", DisplayKind.PERCENT),
    BLEEDING_VULNERABILITY("Уязвимость к Урону Кровотечения", DisplayKind.PERCENT),
    CURSE_VULNERABILITY("Уязвимость к Урону Проклятия", DisplayKind.PERCENT),
    WITHER_VULNERABILITY("Уязвимость к Урону Иссушения", DisplayKind.PERCENT),
    DECAY_VULNERABILITY("Уязвимость к Урону Разложения", DisplayKind.PERCENT),
    ALL_DAMAGE_VULNERABILITY("Уязвимость ко всему урону", DisplayKind.PERCENT),

    ALL_DAMAGE_MULTIPLIER("Весь урон", DisplayKind.MULTIPLIER),
    CLICK_CRIT_CHANCE("Шанс крита", DisplayKind.PERCENT),
    CLICK_CRIT_DAMAGE_MULTIPLIER("Крит. урон при клике", DisplayKind.MULTIPLIER),

    // Current RE:volution names replace the old reference GOLD/RESOURCE aliases.
    COIN_REWARD_PERCENT("Добыча монет", DisplayKind.PERCENT),
    RESOURCE_REWARD_PERCENT("Добыча ресурса", DisplayKind.PERCENT),

    // Targeted stat: a concrete rune/family target is stored separately when used.
    CARD_POWER_FLAT("Сила рун", DisplayKind.FLAT),

    // Reserved content types. Their gameplay systems will be wired later.
    HERO_XP_MULTIPLIER("Опыт героев", DisplayKind.MULTIPLIER),
    MAGIC_SPHERE_DURATION("Время работы магических сфер", DisplayKind.SECONDS);

    enum DisplayKind {
        FLAT,
        MULTIPLIER,
        PERCENT,
        SECONDS
    }

    final String displayName;
    final DisplayKind displayKind;

    RuneStatType(String displayName, DisplayKind displayKind) {
        this.displayName = displayName;
        this.displayKind = displayKind;
    }

    String format(double value) {
        switch (displayKind) {
            case MULTIPLIER:
                // ASCII x is used deliberately: the active 1.12.2 GUI font renders
                // the Unicode multiplication sign as '?'.
                return "x" + compact(value);
            case PERCENT:
                return "+" + compact(value) + "%";
            case SECONDS:
                return "+" + compact(value) + " сек";
            case FLAT:
            default:
                return "+" + compact(value);
        }
    }

    boolean isDamageMultiplier() {
        switch (this) {
            case CLICK_DAMAGE_MULTIPLIER:
            case POISON_DAMAGE_MULTIPLIER:
            case BURNING_DAMAGE_MULTIPLIER:
            case BLEEDING_DAMAGE_MULTIPLIER:
            case CURSE_DAMAGE_MULTIPLIER:
            case WITHER_DAMAGE_MULTIPLIER:
            case DECAY_DAMAGE_MULTIPLIER:
                return true;
            default:
                return false;
        }
    }

    static String compact(double value) {
        return LargeNumberFormatter.format(value);
    }
}
