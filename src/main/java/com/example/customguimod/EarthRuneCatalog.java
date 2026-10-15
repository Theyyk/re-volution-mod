package com.example.customguimod;

/** First status: fixed rune positions in reading order. */
final class EarthRuneCatalog {
    static final class Definition {
        final String name;
        final String type;
        final String iconTexture;
        final RuneStat[] stats;

        Definition(String name, String type, String iconFile, RuneStat... stats) {
            if (stats != null && stats.length > 3) {
                throw new IllegalArgumentException("A rune may define at most three stats");
            }
            this.name = name;
            this.type = type;
            this.iconTexture = "customguimod:textures/gui/runes/" + iconFile;
            this.stats = stats == null ? new RuneStat[0] : stats.clone();
        }
    }

    private static RuneStat clickDamage() {
        return RuneStat.scaled(RuneStatType.CLICK_DAMAGE_FLAT, 1.0D);
    }

    private static RuneStat poisonDamage() {
        return RuneStat.scaled(RuneStatType.POISON_DAMAGE_FLAT, 1.0D);
    }

    private static RuneStat coinReward() {
        return RuneStat.scaled(RuneStatType.COIN_REWARD_PERCENT, 1.0D);
    }

    private static final Definition[] RUNES = {
        new Definition("Кинжал убийцы", "Клик", "killer_dagger.png", clickDamage()),
        new Definition("Кинжал убийцы", "Клик", "killer_dagger.png", clickDamage()),
        new Definition("Кинжал убийцы", "Клик", "killer_dagger.png", clickDamage()),
        new Definition("Кинжал убийцы", "Клик", "killer_dagger.png", clickDamage()),
        new Definition("Накидка убийцы", "Клик", "killer_cloak.png", clickDamage()),
        new Definition("Накидка убийцы", "Клик", "killer_cloak.png", clickDamage()),
        new Definition("Накидка убийцы", "Клик", "killer_cloak.png", clickDamage()),
        new Definition("Перчатка скрытого удара", "Клик", "hidden_strike_glove.png", clickDamage()),
        new Definition("Камень заточки", "Клик", "sharpening_stone.png", clickDamage()),
        new Definition("Камень заточки", "Клик", "sharpening_stone.png", clickDamage()),
        new Definition("Амулет ярости", "Клик", "rage_amulet.png", clickDamage()),
        new Definition("Токсичный осколок", "Клик / Яд", "toxic_shard.png", clickDamage(), poisonDamage()),
        new Definition("Токсичный осколок", "Клик / Яд", "toxic_shard.png", clickDamage(), poisonDamage()),
        new Definition("Клык змеи", "Яд", "snake_fang.png", poisonDamage()),
        new Definition("Клык змеи", "Яд", "snake_fang.png", poisonDamage()),
        new Definition("Клык змеи", "Яд", "snake_fang.png", poisonDamage()),
        new Definition("Клык змеи", "Яд", "snake_fang.png", poisonDamage()),
        new Definition("Печать заражения", "Яд", "infection_seal.png", poisonDamage()),
        new Definition("Печать заражения", "Яд", "infection_seal.png", poisonDamage()),
        new Definition("Печать заражения", "Яд", "infection_seal.png", poisonDamage()),
        new Definition("Усилитель токсина", "Яд", "toxin_booster.png", poisonDamage()),
        new Definition("Усилитель токсина", "Яд", "toxin_booster.png", poisonDamage()),
        new Definition("Метка заражения", "Яд", "infection_mark.png", poisonDamage()),
        new Definition("Метка заражения", "Яд", "infection_mark.png", poisonDamage()),
        new Definition("Метка заражения", "Яд", "infection_mark.png", poisonDamage()),
        new Definition("Реликвия охотника", "Ресурс", "hunter_relic.png", coinReward())
    };

    // Legacy icon indices are retained for compatibility; PNG paths live in each definition.
    private static final int[] ICONS = {
        0, 0, 0, 0, 1, 1, 1, 2, 3, 3, 4, 5, 5,
        6, 6, 6, 6, 7, 7, 7, 8, 8, 9, 9, 9, 10
    };

    static int iconIndex(int slot) {
        return slot < 0 || slot >= ICONS.length ? -1 : ICONS[slot];
    }

    static int size() { return RUNES.length; }

    static Definition at(int slot) {
        return slot < 0 || slot >= RUNES.length ? null : RUNES[slot];
    }
}
