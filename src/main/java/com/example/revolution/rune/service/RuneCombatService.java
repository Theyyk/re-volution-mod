package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneDamageChannel;
import com.example.revolution.rune.definition.RuneStatType;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneCombatMath;
import com.example.revolution.rune.rules.RuneDamageMath;
import com.example.revolution.rune.state.RuneCombatState;
import com.example.revolution.rune.state.RuneInventory;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Server-authoritative resolver for rune damage, proc and temporary effects. */
public final class RuneCombatService {
    private static final class Aggregate {
        final RuneDamageMath.Snapshot damage = new RuneDamageMath.Snapshot();
        double clickProcChancePercent;
        double clickProcDamageMultiplier = 1.0D;
        double temporaryAllDamageMultiplier = 1.0D;
        boolean hasCollection;
    }

    private final PlayerRepository players;
    private final RuneEnhancementRepository enhancements;
    private final RuneCombatState state;
    private final RuneEffectBalanceService effectBalance;

    public RuneCombatService(PlayerRepository players, RuneEnhancementRepository enhancements, RuneCombatState state) {
        this(players, enhancements, state,
                new RuneEffectBalanceService(BalanceBootstrap.activateBundledDefaults()));
    }

    public RuneCombatService(
            PlayerRepository players,
            RuneEnhancementRepository enhancements,
            RuneCombatState state,
            RuneEffectBalanceService effectBalance) {
        if (players == null || enhancements == null || state == null || effectBalance == null) {
            throw new IllegalArgumentException("rune combat dependencies are required");
        }
        this.players = players;
        this.enhancements = enhancements;
        this.state = state;
        this.effectBalance = effectBalance;
    }

    /** Compatibility facade for the existing CLICK + POISON combat path. */
    public RuneCombatMath.Snapshot resolve(UUID uuid, long worldTick) {
        Aggregate aggregate = resolveAggregate(uuid);
        if (aggregate == null || !aggregate.hasCollection) return null;
        RuneCombatMath.Snapshot snapshot = legacySnapshot(aggregate);
        if (snapshot.rageMultiplier > 1.0D && isRageActive(uuid, worldTick)) snapshot.rageActive = true;
        return snapshot;
    }

    /** Compatibility facade without transient effects. */
    public RuneCombatMath.Snapshot resolveStable(UUID uuid) {
        Aggregate aggregate = resolveAggregate(uuid);
        return aggregate == null || !aggregate.hasCollection ? null : legacySnapshot(aggregate);
    }

    /** Generic permanent damage snapshot for all seven canonical channels. */
    public RuneDamageMath.Snapshot resolveDamageStable(UUID uuid) {
        Aggregate aggregate = resolveAggregate(uuid);
        return aggregate == null || !aggregate.hasCollection ? null : aggregate.damage;
    }

    /** Generic damage snapshot including the currently active temporary all-damage effect. */
    public RuneDamageMath.Snapshot resolveDamage(UUID uuid, long worldTick) {
        Aggregate aggregate = resolveAggregate(uuid);
        if (aggregate == null || !aggregate.hasCollection) return null;
        if (aggregate.temporaryAllDamageMultiplier > 1.0D && isRageActive(uuid, worldTick)) {
            aggregate.damage.temporaryAllDamageActive = true;
            aggregate.damage.temporaryAllDamageMultiplier = aggregate.temporaryAllDamageMultiplier;
        }
        return aggregate.damage;
    }

    private Aggregate resolveAggregate(UUID uuid) {
        if (uuid == null) return null;
        Aggregate aggregate = new Aggregate();
        for (RuneCollectionId collectionId : availableCollections(uuid)) {
            if (!isCollectionUnlocked(uuid, collectionId)) continue;
            aggregate.hasCollection = true;
            resolveCollection(uuid, collectionId, aggregate);
        }
        return aggregate;
    }

    private void resolveCollection(UUID uuid, RuneCollectionId collectionId, Aggregate aggregate) {
        int collectionIndex = collectionId.legacyIndex();
        RuneInventory inventory = new RuneInventory();
        List<RuneRecord> runes = players.getRunesInCollection(uuid, collectionIndex);
        for (RuneRecord rune : runes) {
            if (rune == null) continue;
            inventory.set(rune.slot, rune.runeIndex, rune.layer);
        }

        int[] enhancementLevels = enhancements.getLevels(uuid, collectionIndex);
        Map<RuneDamageChannel, Double> effectiveness = new EnumMap<>(RuneDamageChannel.class);
        Map<RuneDamageChannel, Double> enhancementEffectiveness = new EnumMap<>(RuneDamageChannel.class);

        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            RuneInventory.Entry owned = inventory.getBoost(rune);
            if (owned == null) continue;
            int enhancementLevel = enhancementLevel(enhancementLevels, rune);
            for (RuneEffectBalanceService.ResolvedEffect effect : effectBalance.effects(
                    collectionId, rune, owned.layer, enhancementLevel)) {
                RuneDamageChannel target = effectChannel(effect);
                if (target == null) continue;
                if (effect.type == RuneEffectBalanceService.EffectType.EFFECTIVENESS) {
                    multiply(effectiveness, target, effect.primaryValue);
                } else if (effect.type == RuneEffectBalanceService.EffectType.ENHANCEMENT_EFFECTIVENESS) {
                    multiply(enhancementEffectiveness, target, effect.primaryValue);
                }
            }
        }

        for (Map.Entry<RuneDamageChannel, Double> entry : effectiveness.entrySet()) {
            aggregate.damage.channel(entry.getKey()).effectivenessMultiplier *= Math.max(0.0D, entry.getValue());
        }

        Map<String, Double> targetedBasePower = new HashMap<>();
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            RuneInventory.Entry owned = inventory.getBoost(rune);
            if (owned == null) continue;
            int enhancementLevel = enhancementLevel(enhancementLevels, rune);
            List<RuneEffectBalanceService.ResolvedEffect> enhanced = effectBalance.effects(
                    collectionId, rune, owned.layer, enhancementLevel);
            List<RuneEffectBalanceService.ResolvedEffect> base = enhancementLevel == 0
                    ? enhanced : effectBalance.effects(collectionId, rune, owned.layer, 0);
            for (int index = 0; index < enhanced.size(); index++) {
                RuneEffectBalanceService.ResolvedEffect effect = enhanced.get(index);
                if (effect.type != RuneEffectBalanceService.EffectType.TARGETED_POWER) continue;
                RuneEffectBalanceService.ResolvedEffect baseline = index < base.size() ? base.get(index) : effect;
                double primary = enhancementAdjusted(effect.primaryValue, baseline.primaryValue,
                        effectChannel(effect), enhancementEffectiveness);
                targetedBasePower.merge(effect.targetSelector, Math.max(0.0D, primary), Double::sum);
            }
        }

        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            RuneInventory.Entry owned = inventory.getBoost(rune);
            if (owned == null) continue;
            int enhancementLevel = enhancementLevel(enhancementLevels, rune);
            List<RuneEffectBalanceService.ResolvedEffect> enhanced = effectBalance.effects(
                    collectionId, rune, owned.layer, enhancementLevel);
            List<RuneEffectBalanceService.ResolvedEffect> base = enhancementLevel == 0
                    ? enhanced : effectBalance.effects(collectionId, rune, owned.layer, 0);
            for (int index = 0; index < enhanced.size(); index++) {
                RuneEffectBalanceService.ResolvedEffect effect = enhanced.get(index);
                RuneEffectBalanceService.ResolvedEffect baseline = index < base.size() ? base.get(index) : effect;
                RuneDamageChannel target = effectChannel(effect);
                double primary = enhancementAdjusted(effect.primaryValue, baseline.primaryValue,
                        target, enhancementEffectiveness);
                double secondary = enhancementAdjusted(effect.secondaryValue, baseline.secondaryValue,
                        target, enhancementEffectiveness);
                switch (effect.type) {
                    case STAT:
                        if (effect.statType == null) {
                            throw new IllegalStateException("STAT rune effect requires stat_type for " + effect.group);
                        }
                        double value = primary;
                        if (effect.acceptsTargetedPower) {
                            value += targetedBasePower.getOrDefault(effect.group, 0.0D) * effect.targetedPowerScale;
                        }
                        accumulate(aggregate.damage, effect.statType, value);
                        break;
                    case CLICK_DAMAGE_PROC:
                        if (collectionId == RuneCollectionId.COLLECTION_01
                                || "CLICK".equalsIgnoreCase(effect.targetSelector)) {
                            aggregate.clickProcChancePercent = Math.max(
                                    aggregate.clickProcChancePercent, primary);
                            aggregate.clickProcDamageMultiplier = Math.max(
                                    aggregate.clickProcDamageMultiplier, secondary);
                        }
                        break;
                    case TEMPORARY_ALL_DAMAGE_MULTIPLIER:
                    case TEMPORARY_DAMAGE_MULTIPLIER:
                        aggregate.temporaryAllDamageMultiplier = Math.max(
                                aggregate.temporaryAllDamageMultiplier, primary);
                        break;
                    case TARGETED_POWER:
                    case EFFECTIVENESS:
                    case ENHANCEMENT_EFFECTIVENESS:
                    case DELAYED_DAMAGE_BONUS:
                    case TEMPORARY_EFFECT_DURATION:
                    default:
                        break;
                }
            }
        }
    }

    private RuneCombatMath.Snapshot legacySnapshot(Aggregate aggregate) {
        RuneCombatMath.Snapshot snapshot = new RuneCombatMath.Snapshot();
        RuneDamageMath.ChannelStats click = aggregate.damage.existing(RuneDamageChannel.CLICK);
        RuneDamageMath.ChannelStats poison = aggregate.damage.existing(RuneDamageChannel.POISON);
        if (click != null) {
            snapshot.clickFlat = click.flat;
            snapshot.clickMultiplier = click.multiplier;
            snapshot.clickVulnerabilityPercent = click.vulnerabilityPercent;
        }
        if (poison != null) {
            snapshot.poisonFlat = poison.flat;
            snapshot.poisonMultiplier = poison.multiplier;
            snapshot.poisonVulnerabilityPercent = poison.vulnerabilityPercent;
        }
        snapshot.allDamageVulnerabilityPercent = aggregate.damage.allDamageVulnerabilityPercent;
        snapshot.allDamageMultiplier = aggregate.damage.allDamageMultiplier;
        snapshot.procChancePercent = aggregate.clickProcChancePercent;
        snapshot.procDamageMultiplier = aggregate.clickProcDamageMultiplier;
        snapshot.rageMultiplier = aggregate.temporaryAllDamageMultiplier;
        return snapshot;
    }

    public boolean rollProc(RuneCombatMath.Snapshot snapshot) {
        if (snapshot == null || snapshot.procChancePercent <= 0.0D) return false;
        return ThreadLocalRandom.current().nextDouble(100.0D) < Math.min(100.0D, snapshot.procChancePercent);
    }

    public void activateRageOnMobHit(UUID uuid, long worldTick) {
        if (uuid == null) return;
        for (RuneCollectionId collectionId : availableCollections(uuid)) {
            if (!isCollectionUnlocked(uuid, collectionId)) continue;
            RuneInventory inventory = new RuneInventory();
            for (RuneRecord rune : players.getRunesInCollection(uuid, collectionId.legacyIndex())) {
                if (rune == null) continue;
                inventory.set(rune.slot, rune.runeIndex, rune.layer);
            }
            int[] enhancementLevels = enhancements.getLevels(uuid, collectionId.legacyIndex());
            double durationMultiplier = temporaryDurationMultiplier(collectionId, inventory, enhancementLevels);

            for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
                RuneInventory.Entry owned = inventory.getBoost(rune);
                if (owned == null) continue;
                int enhancementLevel = enhancementLevel(enhancementLevels, rune);
                for (RuneEffectBalanceService.ResolvedEffect effect : effectBalance.effects(
                        collectionId, rune, owned.layer, enhancementLevel)) {
                    if ((effect.type != RuneEffectBalanceService.EffectType.TEMPORARY_ALL_DAMAGE_MULTIPLIER
                            && effect.type != RuneEffectBalanceService.EffectType.TEMPORARY_DAMAGE_MULTIPLIER)
                            || !"HIT_MOB".equalsIgnoreCase(effect.targetSelector)
                            || effect.maxStacks < 1) continue;
                    long durationTicks = Math.max(1L, Math.round(effect.durationSeconds * durationMultiplier * 20.0D));
                    state.activate(uuid, worldTick + durationTicks);
                    return;
                }
            }
        }
    }

    public boolean isRageActive(UUID uuid, long worldTick) {
        return state.isActive(uuid, worldTick);
    }

    private double temporaryDurationMultiplier(
            RuneCollectionId collectionId, RuneInventory inventory, int[] enhancementLevels) {
        double multiplier = 1.0D;
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            RuneInventory.Entry owned = inventory.getBoost(rune);
            if (owned == null) continue;
            int enhancementLevel = enhancementLevel(enhancementLevels, rune);
            for (RuneEffectBalanceService.ResolvedEffect effect : effectBalance.effects(
                    collectionId, rune, owned.layer, enhancementLevel)) {
                if (effect.type == RuneEffectBalanceService.EffectType.TEMPORARY_EFFECT_DURATION) {
                    multiplier *= Math.max(0.0D, effect.primaryValue);
                }
            }
        }
        return multiplier;
    }

    private RuneCollectionId[] availableCollections(UUID uuid) {
        if (players instanceof RuneCollectionStateRepository) return RuneCollectionId.values();
        int legacyCount = Math.min(players.getCollectionNames(uuid).size(), RuneCollectionId.values().length);
        RuneCollectionId[] ids = new RuneCollectionId[legacyCount];
        for (int i = 0; i < legacyCount; i++) ids[i] = RuneCollectionId.fromLegacyIndex(i);
        return ids;
    }

    private boolean isCollectionUnlocked(UUID uuid, RuneCollectionId id) {
        if (id == null) return false;
        if (!(players instanceof RuneCollectionStateRepository)) return true;
        Set<RuneCollectionId> unlocked = ((RuneCollectionStateRepository) players).getUnlockedCollections(uuid);
        return unlocked != null && unlocked.contains(id);
    }

    private int enhancementLevel(int[] levels, int rune) {
        return levels != null && rune >= 0 && rune < levels.length ? Math.max(0, levels[rune]) : 0;
    }

    private void multiply(Map<RuneDamageChannel, Double> values, RuneDamageChannel channel, double multiplier) {
        if (channel == null || !Double.isFinite(multiplier)) return;
        values.put(channel, values.getOrDefault(channel, 1.0D) * Math.max(0.0D, multiplier));
    }

    private double enhancementAdjusted(
            double enhancedValue,
            double baseValue,
            RuneDamageChannel channel,
            Map<RuneDamageChannel, Double> effectiveness) {
        if (channel == null) return enhancedValue;
        double multiplier = effectiveness.getOrDefault(channel, 1.0D);
        return baseValue + (enhancedValue - baseValue) * Math.max(0.0D, multiplier);
    }

    private RuneDamageChannel effectChannel(RuneEffectBalanceService.ResolvedEffect effect) {
        if (effect == null) return null;
        if (effect.statType != null) {
            RuneDamageChannel channel = RuneDamageChannel.fromDamageStat(effect.statType);
            if (channel != null) return channel;
            channel = RuneDamageChannel.fromVulnerabilityStat(effect.statType);
            if (channel != null) return channel;
        }
        if (effect.targetSelector == null) return null;
        try {
            return RuneDamageChannel.valueOf(effect.targetSelector.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void accumulate(RuneDamageMath.Snapshot snapshot, RuneStatType type, double value) {
        if (!Double.isFinite(value) || type == null) return;

        RuneDamageChannel damageChannel = RuneDamageChannel.fromDamageStat(type);
        if (damageChannel != null) {
            RuneDamageMath.ChannelStats channel = snapshot.channel(damageChannel);
            if (type.isDamageMultiplier()) channel.multiplier *= Math.max(0.0D, value);
            else channel.flat += Math.max(0.0D, value);
            return;
        }

        RuneDamageChannel vulnerabilityChannel = RuneDamageChannel.fromVulnerabilityStat(type);
        if (vulnerabilityChannel != null) {
            snapshot.channel(vulnerabilityChannel).vulnerabilityPercent += value;
            return;
        }

        switch (type) {
            case ALL_DAMAGE_VULNERABILITY:
                snapshot.allDamageVulnerabilityPercent += value;
                break;
            case ALL_DAMAGE_MULTIPLIER:
                snapshot.allDamageMultiplier *= Math.max(0.0D, value);
                break;
            default:
                // Economy/Pet XP and future non-damage channels intentionally do not affect combat.
                break;
        }
    }
}
