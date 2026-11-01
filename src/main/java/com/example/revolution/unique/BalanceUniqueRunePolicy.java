package com.example.revolution.unique;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.effect.EffectResolver;
import com.example.revolution.unique.UniqueRuneModel.AcquisitionType;
import com.example.revolution.unique.UniqueRuneModel.CollectionDefinition;
import com.example.revolution.unique.UniqueRuneModel.CollectionId;
import com.example.revolution.unique.UniqueRuneModel.DuplicatePolicy;
import com.example.revolution.unique.UniqueRuneModel.Effect;
import com.example.revolution.unique.UniqueRuneModel.RuneDefinition;
import java.math.BigDecimal;
import java.util.ArrayList;
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
        int index = runeIndex(runeId);
        BigDecimal base = number("unique_rune.crystal.enhancement_base_cost");
        BigDecimal growth = decimalList("unique_rune.crystal.enhancement_cost_growth").get(index);
        return base.multiply(BigDecimal.ONE.add(growth.multiply(BigDecimal.valueOf(Math.max(0, currentLevel)))));
    }
    public String dailyExtraResource() { return value("unique_rune.daily.extra_resource"); }
    public BigDecimal dailyExtraAmount() { return number("unique_rune.daily.extra_amount"); }
    public String zbtStatus() { return value("unique_rune.zbt_status"); }

    private CollectionDefinition build(CollectionId id, AcquisitionType acquisition, DuplicatePolicy duplicate, String stem) {
        String p = prefix(id);
        List<String> stats = list(p + ".effect_stats");
        List<BigDecimal> bases = decimalList(p + ".base_values");
        List<BigDecimal> rarityScales = decimalList(p + ".rarity_scales");
        List<BigDecimal> enhancementSteps = id == CollectionId.CRYSTAL_COLLECTION
                ? decimalList(p + ".enhancement_steps") : Collections.<BigDecimal>emptyList();
        require26(stats, p + ".effect_stats"); require26(bases, p + ".base_values"); require26(rarityScales, p + ".rarity_scales");
        if (id == CollectionId.CRYSTAL_COLLECTION) require26(enhancementSteps, p + ".enhancement_steps");
        List<RuneDefinition> runes = new ArrayList<>();
        for (int i = 0; i < UniqueRuneModel.RUNES_PER_COLLECTION; i++) {
            String runeId = String.format("%s_%02d", stem, i + 1);
            EffectResolver.Stat stat = EffectResolver.Stat.valueOf(stats.get(i));
            BigDecimal enhancementStep = id == CollectionId.CRYSTAL_COLLECTION ? enhancementSteps.get(i) : BigDecimal.ZERO;
            List<Effect> effects = new ArrayList<>();
            effects.add(new Effect(stat, bases.get(i), rarityScales.get(i), enhancementStep));
            if (i == 25) effects.add(new Effect(EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER,
                    bases.get(i).divide(BigDecimal.valueOf(2)), rarityScales.get(i), enhancementStep));
            runes.add(new RuneDefinition(runeId, id, technicalName(stem, i + 1), effects));
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
    private static String technicalName(String stem, int number) {
        return Character.toUpperCase(stem.charAt(0)) + stem.substring(1) + " Rune " + String.format("%02d", number);
    }
    private static int runeIndex(String runeId) {
        if (runeId == null || !runeId.startsWith("crystal_") || runeId.length() != 10) throw new IllegalArgumentException("Unknown Crystal Rune: " + runeId);
        try {
            int value = Integer.parseInt(runeId.substring(8));
            if (value < 1 || value > 26) throw new IllegalArgumentException("Unknown Crystal Rune: " + runeId);
            return value - 1;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Unknown Crystal Rune: " + runeId); }
    }
    private static void require26(List<?> values, String key) {
        if (values.size() != UniqueRuneModel.RUNES_PER_COLLECTION) throw new IllegalArgumentException(key + " must contain 26 values");
    }
    private List<String> list(String key) {
        String[] split = value(key).split(","); List<String> result = new ArrayList<>();
        for (String item : split) result.add(item.trim()); return result;
    }
    private List<BigDecimal> decimalList(String key) {
        List<BigDecimal> result = new ArrayList<>(); for (String item : list(key)) result.add(new BigDecimal(item)); return result;
    }
    private boolean bool(String key) { return Boolean.parseBoolean(value(key)); }
    private BigDecimal number(String key) { return new BigDecimal(value(key)); }
    private String value(String key) { return balance.getActiveValue(key); }
}
