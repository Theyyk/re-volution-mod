package com.example.revolution.aura;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.effect.EffectResolver;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Reads every Aura numeric/toggle value from Balance Core. */
public final class BalanceAuraPolicy implements AuraPolicy {
    public static final List<EffectResolver.Stat> SUPPORTED_EFFECTS = Collections.unmodifiableList(Arrays.asList(
            EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER,
            EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER,
            EffectResolver.Stat.BURNING_DAMAGE_MULTIPLIER,
            EffectResolver.Stat.BLEEDING_DAMAGE_MULTIPLIER,
            EffectResolver.Stat.CURSE_DAMAGE_MULTIPLIER,
            EffectResolver.Stat.WITHER_DAMAGE_MULTIPLIER,
            EffectResolver.Stat.DECAY_DAMAGE_MULTIPLIER,
            EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER,
            EffectResolver.Stat.VULNERABILITY_MULTIPLIER,
            EffectResolver.Stat.EFFECTIVENESS_MULTIPLIER,
            EffectResolver.Stat.COIN_REWARD_MULTIPLIER,
            EffectResolver.Stat.RUNE_RESOURCE_REWARD_MULTIPLIER));

    private final BalanceService balance;

    public BalanceAuraPolicy(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance is required");
        this.balance = balance;
    }

    @Override public boolean featureEnabled() { return bool("aura.feature.aura_system"); }
    @Override public boolean rarityEnabled(AuraModel.Rarity rarity) { return bool(rarityKey(rarity, "enabled")); }
    @Override public BigDecimal dropWeight(AuraModel.Rarity rarity) { return decimal(rarityKey(rarity, "drop_weight")); }
    @Override public int minRolls(AuraModel.Rarity rarity) { return integer(rarityKey(rarity, "min_rolls")); }
    @Override public int maxRolls(AuraModel.Rarity rarity) { return integer(rarityKey(rarity, "max_rolls")); }
    @Override public int craftToNextCost(AuraModel.Rarity rarity) { return integer(rarityKey(rarity, "craft_to_next_cost")); }
    @Override public int dismantleToPreviousCount(AuraModel.Rarity rarity) { return integer(rarityKey(rarity, "dismantle_to_previous_count")); }
    @Override public boolean effectEnabled(AuraModel.Rarity rarity, EffectResolver.Stat stat) { return bool(effectKey(rarity, stat, "enabled")); }
    @Override public BigDecimal effectWeight(AuraModel.Rarity rarity, EffectResolver.Stat stat) { return decimal(effectKey(rarity, stat, "weight")); }
    @Override public BigDecimal minEffect(AuraModel.Rarity rarity, EffectResolver.Stat stat) { return decimal(effectKey(rarity, stat, "min_percent")).movePointLeft(2); }
    @Override public BigDecimal maxEffect(AuraModel.Rarity rarity, EffectResolver.Stat stat) { return decimal(effectKey(rarity, stat, "max_percent")).movePointLeft(2); }
    @Override public int keyCrystalPrice() { return integer("aura.key.crystal_price"); }

    @Override public List<Integer> allowedOpenQuantities() {
        String raw = balance.getActiveValue("aura.chest.allowed_open_quantities");
        List<Integer> result = new ArrayList<>();
        for (String token : raw.split(",")) {
            try {
                int value = Integer.parseInt(token.trim());
                if (value > 0 && !result.contains(value)) result.add(value);
            } catch (RuntimeException ignored) { }
        }
        return Collections.unmodifiableList(result);
    }

    private String rarityKey(AuraModel.Rarity rarity, String suffix) {
        return "aura.rarity." + rarity.name().toLowerCase(Locale.ROOT) + "." + suffix;
    }

    private String effectKey(AuraModel.Rarity rarity, EffectResolver.Stat stat, String suffix) {
        return "aura.effect." + rarity.name().toLowerCase(Locale.ROOT) + "." + stat.name().toLowerCase(Locale.ROOT) + "." + suffix;
    }

    private boolean bool(String id) { return Boolean.parseBoolean(balance.getActiveValue(id)); }
    private int integer(String id) { return new BigDecimal(balance.getActiveValue(id)).intValueExact(); }
    private BigDecimal decimal(String id) { return new BigDecimal(balance.getActiveValue(id)); }
}
