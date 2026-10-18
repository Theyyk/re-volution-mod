package com.example.customguimod.rune;
import com.example.customguimod.persistence.MongoManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Server-authoritative resolver for rune damage, proc and temporary rage effects. */
public final class RuneCombatService {
    private static final Map<UUID, Long> RAGE_EXPIRY_TICKS = new ConcurrentHashMap<>();

    private RuneCombatService() {}

    public static RuneCombatMath.Snapshot resolve(UUID uuid, long worldTick) {
        RuneCombatMath.Snapshot snapshot = resolveStable(uuid);
        if (snapshot == null) return null;
        if (snapshot.rageMultiplier > 1.0D && isRageActive(uuid, worldTick)) snapshot.rageActive = true;
        return snapshot;
    }

    /**
     * Resolves permanent combat power from every configured rune collection. The selected collection
    public  * only controls which collection is being viewed/edited in the GUI; it does not disable combat
     * power already owned in other configured collections.
     */
    public static RuneCombatMath.Snapshot resolveStable(UUID uuid) {
        if (uuid == null) return null;
        int collectionCount = MongoManager.getCollectionNames(uuid).size();
        RuneCombatMath.Snapshot aggregate = null;
        for (int collectionIndex = 0; collectionIndex < collectionCount; collectionIndex++) {
            if (RuneStatusRegistry.statusForCollection(collectionIndex) != RuneStatusRegistry.STATUS_POISON) continue;
            RuneCombatMath.Snapshot collection = resolvePoisonCollection(uuid, collectionIndex);
            if (collection == null) continue;
            if (aggregate == null) aggregate = new RuneCombatMath.Snapshot();
            merge(aggregate, collection);
        }
        return aggregate;
    }

    private static RuneCombatMath.Snapshot resolvePoisonCollection(UUID uuid, int collectionIndex) {
        RuneInventory inventory = new RuneInventory();
        List<MongoManager.SavedRune> runes = MongoManager.getRunesInCollection(uuid, collectionIndex);
        for (MongoManager.SavedRune rune : runes) {
            if (rune == null) continue;
            inventory.set(rune.slot, rune.runeIndex, rune.layer);
        }

        int[] enhancementLevels = RuneEnhancementManager.getLevels(uuid, collectionIndex);
        Map<String, Double> targetedBasePower = new HashMap<>();

        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            RuneInventory.Entry owned = inventory.getBoost(rune);
            if (owned == null) continue;
            PrimaryRuneCatalog.Definition definition = PrimaryRuneCatalog.at(rune);
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
            PrimaryRuneCatalog.Definition definition = PrimaryRuneCatalog.at(rune);
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
                        snapshot.procDamageMultiplier = Math.max(
                                snapshot.procDamageMultiplier,
                                effect.resolvedProcDamageMultiplier(owned.layer, enhancementMultiplier)
                        );
                        break;
                    case TEMPORARY_ALL_DAMAGE_MULTIPLIER:
                        snapshot.rageMultiplier = Math.max(
                                snapshot.rageMultiplier,
                                effect.resolvedTemporaryAllDamageMultiplier(owned.layer, enhancementMultiplier)
                        );
                        break;
                    default:
                        break;
                }
            }
        }

        return snapshot;
    }

    private static void merge(RuneCombatMath.Snapshot target, RuneCombatMath.Snapshot source) {
        target.clickFlat += source.clickFlat;
        target.clickMultiplier *= source.clickMultiplier;
        target.poisonFlat += source.poisonFlat;
        target.poisonMultiplier *= source.poisonMultiplier;
        target.clickVulnerabilityPercent += source.clickVulnerabilityPercent;
        target.poisonVulnerabilityPercent += source.poisonVulnerabilityPercent;
        target.allDamageVulnerabilityPercent += source.allDamageVulnerabilityPercent;
        target.allDamageMultiplier *= source.allDamageMultiplier;
        target.procChancePercent = Math.max(target.procChancePercent, source.procChancePercent);
        target.procDamageMultiplier = Math.max(target.procDamageMultiplier, source.procDamageMultiplier);
        target.rageMultiplier = Math.max(target.rageMultiplier, source.rageMultiplier);
    }

    public static boolean rollProc(RuneCombatMath.Snapshot snapshot) {
        if (snapshot == null || snapshot.procChancePercent <= 0.0D) return false;
        return ThreadLocalRandom.current().nextDouble(100.0D) < Math.min(100.0D, snapshot.procChancePercent);
    }

    public static void activateRageOnMobHit(UUID uuid, long worldTick) {
        if (uuid == null) return;
        int collectionCount = MongoManager.getCollectionNames(uuid).size();
        for (int collectionIndex = 0; collectionIndex < collectionCount; collectionIndex++) {
            if (RuneStatusRegistry.statusForCollection(collectionIndex) != RuneStatusRegistry.STATUS_POISON) continue;

            RuneInventory inventory = new RuneInventory();
            for (MongoManager.SavedRune rune : MongoManager.getRunesInCollection(uuid, collectionIndex)) {
                if (rune == null) continue;
                inventory.set(rune.slot, rune.runeIndex, rune.layer);
            }

            for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
                if (inventory.getBoost(rune) == null) continue;
                PrimaryRuneCatalog.Definition definition = PrimaryRuneCatalog.at(rune);
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
    }

    public static boolean isRageActive(UUID uuid, long worldTick) {
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
