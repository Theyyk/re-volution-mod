package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneDamageChannel;
import com.example.revolution.rune.definition.RuneStatType;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneDamageMath;
import com.example.revolution.rune.state.RuneInventory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Builds one generic damage snapshot from factual rune state across permanently unlocked collections. */
public final class RuneDamageSnapshotService {
    private final PlayerRepository players;
    private final RuneEnhancementRepository enhancements;
    private final RuneEffectBalanceService effects;

    public RuneDamageSnapshotService(
            PlayerRepository players,
            RuneEnhancementRepository enhancements,
            RuneEffectBalanceService effects) {
        if (players == null || enhancements == null || effects == null) {
            throw new IllegalArgumentException("rune damage snapshot dependencies are required");
        }
        this.players = players;
        this.enhancements = enhancements;
        this.effects = effects;
    }

    public RuneDamageMath.Snapshot resolve(UUID uuid) {
        RuneDamageMath.Snapshot snapshot = new RuneDamageMath.Snapshot();
        if (uuid == null) return snapshot;

        Set<RuneCollectionId> unlocked = players instanceof RuneCollectionStateRepository
                ? ((RuneCollectionStateRepository) players).getUnlockedCollections(uuid)
                : null;

        for (RuneCollectionId collectionId : RuneCollectionId.values()) {
            if (unlocked != null && !unlocked.contains(collectionId)) continue;
            accumulateCollection(snapshot, uuid, collectionId);
        }
        return snapshot;
    }

    public double damage(UUID uuid, RuneDamageChannel channel, double baseDamage) {
        return RuneDamageMath.damage(baseDamage, channel, resolve(uuid));
    }

    public double statusDamage(UUID uuid, RuneDamageChannel channel) {
        if (channel == null || channel == RuneDamageChannel.CLICK) return 0.0D;
        return RuneDamageMath.statusDamage(channel, resolve(uuid));
    }

    private void accumulateCollection(
            RuneDamageMath.Snapshot snapshot,
            UUID uuid,
            RuneCollectionId collectionId) {
        RuneInventory inventory = new RuneInventory();
        List<RuneRecord> records = players.getRunesInCollection(uuid, collectionId.legacyIndex());
        for (RuneRecord record : records) {
            if (record != null) inventory.set(record.slot, record.runeIndex, record.layer);
        }
        int[] levels = enhancements.getLevels(uuid, collectionId.legacyIndex());

        Map<String, Double> targetedPower = new HashMap<>();
        for (int slot = 0; slot < RuneInventory.BOOST_TYPE_COUNT; slot++) {
            RuneInventory.Entry owned = inventory.getBoost(slot);
            if (owned == null) continue;
            int level = enhancementLevel(levels, slot);
            for (RuneEffectBalanceService.ResolvedEffect effect : effects.effects(
                    collectionId, slot, owned.layer, level)) {
                if (effect.type == RuneEffectBalanceService.EffectType.TARGETED_POWER
                        && effect.targetSelector != null) {
                    targetedPower.merge(effect.targetSelector, Math.max(0.0D, effect.primaryValue), Double::sum);
                }
            }
        }

        for (int slot = 0; slot < RuneInventory.BOOST_TYPE_COUNT; slot++) {
            RuneInventory.Entry owned = inventory.getBoost(slot);
            if (owned == null) continue;
            int level = enhancementLevel(levels, slot);
            for (RuneEffectBalanceService.ResolvedEffect effect : effects.effects(
                    collectionId, slot, owned.layer, level)) {
                if (effect.type != RuneEffectBalanceService.EffectType.STAT || effect.statType == null) continue;
                double value = effect.primaryValue;
                if (effect.acceptsTargetedPower) {
                    value += targetedPower.getOrDefault(effect.group, 0.0D) * effect.targetedPowerScale;
                }
                accumulate(snapshot, effect.statType, value);
            }
        }
    }

    static void accumulate(RuneDamageMath.Snapshot snapshot, RuneStatType type, double value) {
        if (snapshot == null || type == null || !Double.isFinite(value)) return;

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
                break;
        }
    }

    private static int enhancementLevel(int[] levels, int slot) {
        return levels != null && slot >= 0 && slot < levels.length ? Math.max(0, levels[slot]) : 0;
    }
}
