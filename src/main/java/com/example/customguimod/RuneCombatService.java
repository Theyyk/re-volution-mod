package com.example.customguimod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Server-authoritative resolver for rune damage, proc and temporary rage effects. */
final class RuneCombatService {
    private static final Map<UUID, Long> RAGE_EXPIRY_TICKS = new ConcurrentHashMap<>();

    private RuneCombatService() {}

    static RuneCombatMath.Snapshot resolve(UUID uuid, long worldTick) {
        if (uuid == null) return null;

        int deckIndex = MongoManager.getActiveDeck(uuid);
        if (RuneStatusRegistry.statusForDeck(deckIndex) != RuneStatusRegistry.STATUS_POISON) return null;

        RuneInventory inventory = new RuneInventory();
        List<MongoManager.SavedCard> cards = MongoManager.getCardsInDeck(uuid, deckIndex);
        for (MongoManager.SavedCard card : cards) {
            if (card == null) continue;
            inventory.set(card.slot, card.cardIndex, card.layer);
        }

        int[] enhancementLevels = RuneEnhancementManager.getLevels(uuid, deckIndex);
        Map<String, Double> targetedBasePower = new HashMap<>();

        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            RuneInventory.Entry owned = inventory.getBoost(rune);
            if (owned == null) continue;
            EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(rune);
            if (definition == null) continue;

            int enhancementLevel = enhancementLevel(enhancementLevels, rune);
            double enhancementMultiplier = 1.0D + enhancementLevel * 0.1D;
            for (RuneTargetedPower power : definition.targetedPowers) {
                if (power == null) continue;
                double resolved = power.resolve(owned.layer, enhancementMultiplier);
                targetedBasePower.merge(power.targetKey, Math.max(0.0D, resolved), Double::sum);
            }
        }

        RuneCombatMath.Snapshot snapshot = new RuneCombatMath.Snapshot();
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            RuneInventory.Entry owned = inventory.getBoost(rune);
            if (owned == null) continue;
            EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(rune);
            if (definition == null) continue;

            int enhancementLevel = enhancementLevel(enhancementLevels, rune);
            double enhancementMultiplier = 1.0D + enhancementLevel * 0.1D;
            double basePowerBonus = targetedBasePower.getOrDefault(targetKeyForSlot(rune), 0.0D);

            for (RuneStat stat : definition.stats) {
                if (stat == null) continue;
                boolean targetedFlat = stat.type == RuneStatType.CLICK_DAMAGE_FLAT
                        || stat.type == RuneStatType.POISON_DAMAGE_FLAT;
                double value = stat.resolve(
                        owned.layer,
                        enhancementMultiplier,
                        targetedFlat ? basePowerBonus : 0.0D
                ).finalValue;
                accumulate(snapshot, stat.type, value);
            }

            for (RuneCompositeEffect effect : definition.compositeEffects) {
                if (effect == null) continue;
                switch (effect.kind) {
                    case CLICK_DAMAGE_PROC:
                        snapshot.procChancePercent = Math.max(snapshot.procChancePercent, effect.primaryValue);
                        snapshot.procDamageMultiplier = Math.max(snapshot.procDamageMultiplier, effect.secondaryValue);
                        break;
                    case TEMPORARY_ALL_DAMAGE_MULTIPLIER:
                        snapshot.rageMultiplier = Math.max(snapshot.rageMultiplier, effect.primaryValue);
                        if (isRageActive(uuid, worldTick)) snapshot.rageActive = true;
                        break;
                    default:
                        break;
                }
            }
        }

        return snapshot;
    }

    static boolean rollProc(RuneCombatMath.Snapshot snapshot) {
        if (snapshot == null || snapshot.procChancePercent <= 0.0D) return false;
        return ThreadLocalRandom.current().nextDouble(100.0D) < Math.min(100.0D, snapshot.procChancePercent);
    }

    static void activateRageOnMobHit(UUID uuid, long worldTick) {
        if (uuid == null) return;
        int deckIndex = MongoManager.getActiveDeck(uuid);
        if (RuneStatusRegistry.statusForDeck(deckIndex) != RuneStatusRegistry.STATUS_POISON) return;

        RuneInventory inventory = new RuneInventory();
        for (MongoManager.SavedCard card : MongoManager.getCardsInDeck(uuid, deckIndex)) {
            if (card == null) continue;
            inventory.set(card.slot, card.cardIndex, card.layer);
        }

        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            if (inventory.getBoost(rune) == null) continue;
            EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(rune);
            if (definition == null) continue;
            for (RuneCompositeEffect effect : definition.compositeEffects) {
                if (effect == null
                        || effect.kind != RuneCompositeEffect.Kind.TEMPORARY_ALL_DAMAGE_MULTIPLIER
                        || effect.activation != RuneCompositeEffect.Activation.CLICK_HIT_MOB
                        || effect.maxStacks < 1) continue;
                long durationTicks = Math.max(1L, Math.round(effect.durationSeconds * 20.0D));
                RAGE_EXPIRY_TICKS.put(uuid, worldTick + durationTicks);
                return;
            }
        }
    }

    static boolean isRageActive(UUID uuid, long worldTick) {
        Long expiry = RAGE_EXPIRY_TICKS.get(uuid);
        if (expiry == null) return false;
        if (worldTick < expiry) return true;
        RAGE_EXPIRY_TICKS.remove(uuid, expiry);
        return false;
    }

    private static int enhancementLevel(int[] levels, int rune) {
        return levels != null && rune >= 0 && rune < levels.length ? Math.max(0, levels[rune]) : 0;
    }

    private static String targetKeyForSlot(int slot) {
        if (slot >= 0 && slot <= 3) return "killer_dagger";
        if (slot >= 13 && slot <= 16) return "snake_fang";
        return "";
    }

    private static void accumulate(RuneCombatMath.Snapshot snapshot, RuneStatType type, double value) {
        if (!Double.isFinite(value)) return;
        switch (type) {
            case CLICK_DAMAGE_FLAT:
                snapshot.clickFlat += Math.max(0.0D, value);
                break;
            case CLICK_DAMAGE_MULTIPLIER:
                snapshot.clickMultiplier *= Math.max(0.0D, value);
                break;
            case POISON_DAMAGE_FLAT:
                snapshot.poisonFlat += Math.max(0.0D, value);
                break;
            case POISON_DAMAGE_MULTIPLIER:
                snapshot.poisonMultiplier *= Math.max(0.0D, value);
                break;
            case CLICK_VULNERABILITY:
                snapshot.clickVulnerabilityPercent += value;
                break;
            case POISON_VULNERABILITY:
                snapshot.poisonVulnerabilityPercent += value;
                break;
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
}
