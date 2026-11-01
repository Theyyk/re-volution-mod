package com.example.revolution.unique;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.effect.EffectResolver;
import com.example.revolution.unique.UniqueRuneModel.AcquisitionType;
import com.example.revolution.unique.UniqueRuneModel.CollectionDefinition;
import com.example.revolution.unique.UniqueRuneModel.CollectionId;
import com.example.revolution.unique.UniqueRuneModel.DuplicatePolicy;
import com.example.revolution.unique.UniqueRuneModel.Effect;
import com.example.revolution.unique.UniqueRuneModel.Rarity;
import com.example.revolution.unique.UniqueRuneModel.RuneDefinition;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Data-driven v2.11.0 Unique Rune policy backed entirely by Balance Core values. */
public final class BalanceUniqueRunePolicy {
    private final BalanceService balance;
    private final Map<CollectionId, CollectionDefinition> definitions = new EnumMap<>(CollectionId.class);

    public BalanceUniqueRunePolicy(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance is required");
        this.balance = balance;
        definitions.put(CollectionId.CRYSTAL_COLLECTION, build(CollectionId.CRYSTAL_COLLECTION, AcquisitionType.CRYSTALS, DuplicatePolicy.PEARL, "crystal"));
        definitions.put(CollectionId.DAILY_COLLECTION, build(CollectionId.DAILY_COLLECTION, AcquisitionType.DAILY_REWARD, DuplicatePolicy.EXCLUDE, "daily"));
        definitions.put(CollectionId.OFFERINGS_COLLECTION, build(CollectionId.OFFERINGS_COLLECTION, AcquisitionType.OFFERINGS, DuplicatePolicy.NONE, "offerings"));
    }

    public CollectionDefinition definition(CollectionId id) { return definitions.get(id); }
    public Map<CollectionId, CollectionDefinition> definitions() { return Collections.unmodifiableMap(definitions); }
    public boolean enabled() { return bool("unique_rune.enabled"); }
    public boolean collectionEnabled(CollectionId id) { return enabled() && bool(prefix(id) + ".enabled"); }
    public long unlockMilestone(CollectionId id) { return number(prefix(id) + ".unlock_milestone").longValue(); }
    public BigDecimal crystalPurchaseCost() { return number("unique_rune.crystal.purchase_cost"); }
    public BigDecimal offeringsPurchaseCost() { return number("unique_rune.offerings.purchase_cost"); }
    public BigDecimal duplicatePearl() { return number("unique_rune.crystal.duplicate_pearl"); }
    public BigDecimal rollbackCrystalCost(CollectionId id) { return number(prefix(id) + ".rollback_crystal_cost"); }
    public BigDecimal enhancementPearlCost(String runeId, int currentLevel) {
        BigDecimal base = number("unique_rune.crystal.enhancement_base_cost");
        BigDecimal growth = number("unique_rune.rune." + runeId + ".enhancement_cost_growth");
        return base.multiply(BigDecimal.ONE.add(growth.multiply(BigDecimal.valueOf(Math.max(0, currentLevel)))));
    }
    public String dailyExtraResource() { return value("unique_rune.daily.extra_resource"); }
    public BigDecimal dailyExtraAmount() { return number("unique_rune.daily.extra_amount"); }
    public String zbtStatus() { return value("unique_rune.zbt_status"); }

    private CollectionDefinition build(CollectionId id, AcquisitionType acquisition, DuplicatePolicy duplicate, String stem) {
        List<RuneDefinition> runes = new ArrayList<>();
        List<EffectResolver.Stat> rotation = Arrays.asList(
                EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER,
                EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER,
                EffectResolver.Stat.BURNING_DAMAGE_MULTIPLIER,
                EffectResolver.Stat.BLEEDING_DAMAGE_MULTIPLIER,
                EffectResolver.Stat.CURSE_DAMAGE_MULTIPLIER,
                EffectResolver.Stat.WITHER_DAMAGE_MULTIPLIER,
                EffectResolver.Stat.DECAY_DAMAGE_MULTIPLIER,
                EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER,
                EffectResolver.Stat.COIN_REWARD_MULTIPLIER,
                EffectResolver.Stat.RUNE_DUST_REWARD_MULTIPLIER,
                EffectResolver.Stat.RUNE_SHARDS_REWARD_MULTIPLIER,
                EffectResolver.Stat.RUNE_CORES_REWARD_MULTIPLIER,
                EffectResolver.Stat.VULNERABILITY_MULTIPLIER,
                EffectResolver.Stat.EFFECTIVENESS_MULTIPLIER);
        for (int i = 1; i <= UniqueRuneModel.RUNES_PER_COLLECTION; i++) {
            String runeId = String.format("%s_%02d", stem, i);
            String runePrefix = "unique_rune.rune." + runeId;
            BigDecimal base = number(runePrefix + ".base");
            BigDecimal rarityScale = number(runePrefix + ".rarity_scale");
            BigDecimal enhancementStep = id == CollectionId.CRYSTAL_COLLECTION ? number(runePrefix + ".enhancement_step") : BigDecimal.ZERO;
            EffectResolver.Stat stat = rotation.get((i - 1) % rotation.size());
            List<Effect> effects = new ArrayList<>();
            effects.add(new Effect(stat, base, rarityScale, enhancementStep));
            if (i == 26) effects.add(new Effect(EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER, base.divide(BigDecimal.valueOf(2)), rarityScale, enhancementStep));
            runes.add(new RuneDefinition(runeId, id, value(runePrefix + ".name"), effects));
        }
        return new CollectionDefinition(id, acquisition, duplicate, runes);
    }

    private String prefix(CollectionId id) {
        switch (id) {
            case CRYSTAL_COLLECTION: return "unique_rune.crystal";
            case DAILY_COLLECTION: return "unique_rune.daily";
            case OFFERINGS_COLLECTION: return "unique_rune.offerings";
            default: throw new IllegalArgumentException("unknown collection");
        }
    }
    private boolean bool(String key) { return Boolean.parseBoolean(value(key)); }
    private BigDecimal number(String key) { return new BigDecimal(value(key)); }
    private String value(String key) { return balance.getActiveValue(key); }
}
