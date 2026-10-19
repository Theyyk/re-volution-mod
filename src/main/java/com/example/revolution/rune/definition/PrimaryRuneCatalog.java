package com.example.revolution.rune.definition;

/** First status: fixed rune positions in reading order. */
public final class PrimaryRuneCatalog {
    public static final class Definition {
        public final String name;
        public final String type;
        public final String iconTexture;
        public final RuneStat[] stats;
        public final RuneTargetedPower[] targetedPowers;
        public final RuneCompositeEffect[] compositeEffects;
        public Definition(String name, String type, String iconFile, RuneStat... stats) {
            this(name, type, iconFile, stats, null, null);
        }

        public Definition(String name,
                   String type,
                   String iconFile,
                   RuneStat[] stats,
                   RuneTargetedPower[] targetedPowers,
                   RuneCompositeEffect[] compositeEffects) {
            int statCount = stats == null ? 0 : stats.length;
            int targetedCount = targetedPowers == null ? 0 : targetedPowers.length;
            int compositeCount = compositeEffects == null ? 0 : compositeEffects.length;
            if (statCount + targetedCount + compositeCount > 3) {
                throw new IllegalArgumentException("A rune may define at most three effects");
            }
            this.name = name;
            this.type = type;
            this.iconTexture = "revolution:textures/gui/runes/" + iconFile;
            this.stats = stats == null ? new RuneStat[0] : stats.clone();
            this.targetedPowers = targetedPowers == null ? new RuneTargetedPower[0] : targetedPowers.clone();
            this.compositeEffects = compositeEffects == null ? new RuneCompositeEffect[0] : compositeEffects.clone();
        }
    }

    private static RuneStat clickDamage() {
        return RuneStat.scaled(RuneStatType.CLICK_DAMAGE_FLAT, 1.0D);
    }

    private static RuneStat clickMultiplier() {
        return RuneStat.scaled(RuneStatType.CLICK_DAMAGE_MULTIPLIER, 1.0D);
    }

    private static RuneStat poisonDamage() {
        return RuneStat.scaled(RuneStatType.POISON_DAMAGE_FLAT, 1.0D);
    }

    private static RuneStat poisonMultiplier() {
        return RuneStat.scaled(RuneStatType.POISON_DAMAGE_MULTIPLIER, 1.0D);
    }

    private static RuneStat poisonVulnerability() {
        return RuneStat.scaled(RuneStatType.POISON_VULNERABILITY, 1.0D);
    }

    private static RuneStat coinReward() {
        return RuneStat.scaled(RuneStatType.COIN_REWARD_PERCENT, 1.0D);
    }

    private static RuneTargetedPower daggerPower() {
        return new RuneTargetedPower("killer_dagger", "Кинжал убийцы", 1.0D);
    }

    private static RuneTargetedPower snakeFangPower() {
        return new RuneTargetedPower("snake_fang", "Клык змеи", 1.0D);
    }

    private static RuneCompositeEffect hiddenStrikeProc() {
        // Approved base test values; rank and +N enhancement scale the proc damage multiplier, not its chance.
        return RuneCompositeEffect.clickDamageProc(15.0D, 2.0D);
    }

    private static RuneCompositeEffect rageBuff() {
        // Approved base test value. Rank and +N enhancement scale the damage multiplier.
        return RuneCompositeEffect.temporaryAllDamage(2.0D, 5.0D, 1);
    }

    private static Definition targeted(String name,
                                       String type,
                                       String iconFile,
                                       RuneTargetedPower targetedPower) {
        return new Definition(
                name,
                type,
                iconFile,
                new RuneStat[0],
                new RuneTargetedPower[]{targetedPower},
                new RuneCompositeEffect[0]
        );
    }

    private static Definition composite(String name,
                                        String type,
                                        String iconFile,
                                        RuneCompositeEffect compositeEffect) {
        return new Definition(
                name,
                type,
                iconFile,
                new RuneStat[0],
                new RuneTargetedPower[0],
                new RuneCompositeEffect[]{compositeEffect}
        );
    }

    private static final Definition[] RUNES = {
        new Definition("Кинжал убийцы", "Клик", "killer_dagger.png", clickDamage()),
        new Definition("Кинжал убийцы", "Клик", "killer_dagger.png", clickDamage()),
        new Definition("Кинжал убийцы", "Клик", "killer_dagger.png", clickDamage()),
        new Definition("Кинжал убийцы", "Клик", "killer_dagger.png", clickDamage()),
        new Definition("Накидка убийцы", "Клик", "killer_cloak.png", clickMultiplier()),
        new Definition("Накидка убийцы", "Клик", "killer_cloak.png", clickMultiplier()),
        new Definition("Накидка убийцы", "Клик", "killer_cloak.png", clickMultiplier()),
        composite("Перчатка скрытого удара", "Клик", "hidden_strike_glove.png", hiddenStrikeProc()),
        targeted("Камень заточки", "Клик", "sharpening_stone.png", daggerPower()),
        targeted("Камень заточки", "Клик", "sharpening_stone.png", daggerPower()),
        composite("Амулет ярости", "Клик", "rage_amulet.png", rageBuff()),
        new Definition("Токсичный осколок", "Клик / Яд", "toxic_shard.png", clickMultiplier(), poisonMultiplier()),
        new Definition("Токсичный осколок", "Клик / Яд", "toxic_shard.png", clickMultiplier(), poisonMultiplier()),
        new Definition("Клык змеи", "Яд", "snake_fang.png", poisonDamage()),
        new Definition("Клык змеи", "Яд", "snake_fang.png", poisonDamage()),
        new Definition("Клык змеи", "Яд", "snake_fang.png", poisonDamage()),
        new Definition("Клык змеи", "Яд", "snake_fang.png", poisonDamage()),
        new Definition("Печать заражения", "Яд", "infection_seal.png", poisonMultiplier()),
        new Definition("Печать заражения", "Яд", "infection_seal.png", poisonMultiplier()),
        new Definition("Печать заражения", "Яд", "infection_seal.png", poisonMultiplier()),
        targeted("Усилитель токсина", "Яд", "toxin_booster.png", snakeFangPower()),
        targeted("Усилитель токсина", "Яд", "toxin_booster.png", snakeFangPower()),
        new Definition("Метка заражения", "Яд", "infection_mark.png", poisonVulnerability()),
        new Definition("Метка заражения", "Яд", "infection_mark.png", poisonVulnerability()),
        new Definition("Метка заражения", "Яд", "infection_mark.png", poisonVulnerability()),
        new Definition("Реликвия охотника", "Ресурс", "hunter_relic.png", coinReward())
    };

    // Legacy icon indices are retained for compatibility; PNG paths live in each definition.
    private static final int[] ICONS = {
        0, 0, 0, 0, 1, 1, 1, 2, 3, 3, 4, 5, 5,
        6, 6, 6, 6, 7, 7, 7, 8, 8, 9, 9, 9, 10
    };

    public static int iconIndex(int slot) {
        return slot < 0 || slot >= ICONS.length ? -1 : ICONS[slot];
    }

    public static int size() { return RUNES.length; }

    public static Definition at(int slot) {
        return slot < 0 || slot >= RUNES.length ? null : RUNES[slot];
    }
}
