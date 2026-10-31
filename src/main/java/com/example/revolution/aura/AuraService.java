package com.example.revolution.aura;

import com.example.revolution.gameplay.GameplayEvent;
import com.example.revolution.gameplay.GameplayEventSink;
import com.example.revolution.gameplay.GameplayEventType;
import com.example.revolution.resource.service.ResourceService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static com.example.revolution.resource.definition.ResourceDefinitions.AURA_KEY;

/** Server-authoritative Aura mutations. Client supplies intent and instance ids only. */
public final class AuraService {
    public static final class OpenResult {
        public final List<AuraModel.AuraInstance> generated;
        public final int purchasedKeys;
        public final int spentCrystals;
        OpenResult(List<AuraModel.AuraInstance> generated, int purchasedKeys, int spentCrystals) {
            this.generated = Collections.unmodifiableList(new ArrayList<>(generated));
            this.purchasedKeys = purchasedKeys;
            this.spentCrystals = spentCrystals;
        }
    }

    private final AuraRepository repository;
    private final ResourceService resources;
    private final AuraPolicy policy;
    private final AuraGenerator generator;
    private final Random secureSeedSource = new Random();
    private GameplayEventSink gameplayEvents = GameplayEventSink.NONE;

    public AuraService(AuraRepository repository, ResourceService resources, AuraPolicy policy) {
        if (repository == null || resources == null || policy == null) throw new IllegalArgumentException("Aura dependencies are required");
        this.repository = repository;
        this.resources = resources;
        this.policy = policy;
        this.generator = new AuraGenerator(policy);
    }

    public void setGameplayEventSink(GameplayEventSink gameplayEvents) {
        this.gameplayEvents = gameplayEvents == null ? GameplayEventSink.NONE : gameplayEvents;
    }

    public synchronized AuraModel.PlayerState state(UUID playerUuid) {
        AuraModel.PlayerState state = repository.load(playerUuid);
        state.repairActiveReference();
        return state;
    }

    public synchronized boolean setSkipOpeningAnimation(UUID playerUuid, boolean skip) {
        AuraModel.PlayerState state = state(playerUuid); state.skipOpeningAnimation = skip; repository.save(playerUuid, state); return true;
    }

    public synchronized boolean grantKeys(UUID playerUuid, int amount) {
        boolean granted = amount >= 0 && resources.grantPlayerResource(playerUuid, AURA_KEY, amount);
        if (granted && amount > 0) gameplayEvents.factualEvent(new GameplayEvent(playerUuid, GameplayEventType.AURA_KEY_GAINED, BigDecimal.valueOf(amount), null, "aura"));
        return granted;
    }

    /** Buys exactly the missing keys with Crystals and opens all requested rolls as one validated mutation. */
    public synchronized OpenResult open(UUID playerUuid, int quantity, long nowMillis) {
        requireFeature();
        if (!policy.allowedOpenQuantities().contains(quantity)) throw new IllegalArgumentException("unsupported Aura chest quantity");
        int ownedKeys = resources.getPlayerResource(playerUuid, AURA_KEY);
        int missing = Math.max(0, quantity - ownedKeys);
        int crystalCost = Math.multiplyExact(missing, policy.keyCrystalPrice());
        if (crystalCost > 0 && !resources.canAfford(playerUuid, "crystals", crystalCost)) throw new IllegalStateException("insufficient crystals");

        if (crystalCost > 0 && !resources.spendPlayerResource(playerUuid, "crystals", crystalCost)) throw new IllegalStateException("crystal spend failed");
        if (missing > 0 && !resources.grantPlayerResource(playerUuid, AURA_KEY, missing)) {
            if (crystalCost > 0) resources.grantPlayerResource(playerUuid, "crystals", crystalCost);
            throw new IllegalStateException("Aura Key grant failed");
        }
        if (!resources.spendPlayerResource(playerUuid, AURA_KEY, quantity)) {
            if (missing > 0) resources.spendPlayerResource(playerUuid, AURA_KEY, missing);
            if (crystalCost > 0) resources.grantPlayerResource(playerUuid, "crystals", crystalCost);
            throw new IllegalStateException("Aura Key spend failed");
        }

        AuraModel.PlayerState state = state(playerUuid);
        List<AuraModel.AuraInstance> generated = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            AuraModel.AuraInstance aura = generator.generateFromChest(nextSeed(), nowMillis);
            state.ownedAuras.add(aura); generated.add(aura); state.totalChestAuraDrops++;
        }
        state.totalChestOpens += quantity;
        repository.save(playerUuid, state);
        gameplayEvents.factualEvent(new GameplayEvent(playerUuid, GameplayEventType.AURA_CHEST_OPENED, BigDecimal.valueOf(quantity), null, "aura"));
        for (AuraModel.AuraInstance aura : generated) gameplayEvents.factualEvent(dropEvent(playerUuid, aura));
        return new OpenResult(generated, missing, crystalCost);
    }

    public synchronized void equip(UUID playerUuid, UUID instanceId) {
        requireFeature(); AuraModel.PlayerState state = state(playerUuid);
        if (state.activeAuraId != null) throw new IllegalStateException("unequip current Aura first");
        if (state.find(instanceId) == null) throw new IllegalArgumentException("Aura is not owned");
        state.activeAuraId = instanceId; repository.save(playerUuid, state);
        gameplayEvents.factualEvent(simpleInstanceEvent(playerUuid, GameplayEventType.AURA_EQUIPPED, state.find(instanceId)));
    }

    public synchronized void unequip(UUID playerUuid) {
        requireFeature(); AuraModel.PlayerState state = state(playerUuid); AuraModel.AuraInstance previous = state.active();
        state.activeAuraId = null; repository.save(playerUuid, state);
        if (previous != null) gameplayEvents.factualEvent(simpleInstanceEvent(playerUuid, GameplayEventType.AURA_UNEQUIPPED, previous));
    }

    public synchronized void setPinned(UUID playerUuid, UUID instanceId, boolean pinned) {
        requireFeature(); AuraModel.PlayerState state = state(playerUuid); AuraModel.AuraInstance aura = state.find(instanceId);
        if (aura == null) throw new IllegalArgumentException("Aura is not owned");
        aura.pinned = pinned; repository.save(playerUuid, state);
    }

    public synchronized AuraModel.AuraInstance craft(UUID playerUuid, AuraModel.Rarity rarity, List<UUID> selectedIds, long nowMillis) {
        requireFeature();
        AuraModel.Rarity next = next(rarity); if (next == null) throw new IllegalArgumentException("MYTHIC cannot be crafted upward");
        int required = policy.craftToNextCost(rarity);
        if (required <= 0 || selectedIds == null || selectedIds.size() != required) throw new IllegalArgumentException("invalid craft selection size");
        AuraModel.PlayerState state = state(playerUuid);
        List<AuraModel.AuraInstance> selected = validatedConsumables(state, rarity, selectedIds);
        state.ownedAuras.removeAll(selected);
        AuraModel.AuraInstance result = generator.generate(next, AuraModel.Origin.CRAFT, nextSeed(), nowMillis);
        state.ownedAuras.add(result); repository.save(playerUuid, state);
        gameplayEvents.factualEvent(simpleInstanceEvent(playerUuid, GameplayEventType.AURA_CRAFTED, result));
        return result;
    }

    public synchronized List<AuraModel.AuraInstance> dismantle(UUID playerUuid, UUID instanceId, long nowMillis) {
        requireFeature(); AuraModel.PlayerState state = state(playerUuid); AuraModel.AuraInstance source = state.find(instanceId);
        if (source == null) throw new IllegalArgumentException("Aura is not owned");
        if (source.rarity == AuraModel.Rarity.COMMON) throw new IllegalArgumentException("COMMON cannot be dismantled");
        ensureConsumable(state, source);
        AuraModel.Rarity previous = previous(source.rarity); int count = policy.dismantleToPreviousCount(source.rarity);
        if (count <= 0) throw new IllegalStateException("invalid dismantle return count");
        state.ownedAuras.remove(source);
        List<AuraModel.AuraInstance> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AuraModel.AuraInstance result = generator.generate(previous, AuraModel.Origin.DISMANTLE, nextSeed(), nowMillis);
            state.ownedAuras.add(result); results.add(result);
        }
        repository.save(playerUuid, state);
        gameplayEvents.factualEvent(new GameplayEvent(playerUuid, GameplayEventType.AURA_DISMANTLED, BigDecimal.valueOf(count), metadata(source), "aura"));
        return Collections.unmodifiableList(results);
    }

    /** Selection-only helper. Never mutates or executes craft. */
    public synchronized List<UUID> autoFill(UUID playerUuid, AuraModel.Rarity rarity) {
        int required = policy.craftToNextCost(rarity); AuraModel.PlayerState state = state(playerUuid); List<UUID> result = new ArrayList<>();
        for (AuraModel.AuraInstance aura : state.ownedAuras) {
            if (aura.rarity == rarity && !aura.pinned && !aura.instanceId.equals(state.activeAuraId)) result.add(aura.instanceId);
            if (result.size() == required) break;
        }
        return Collections.unmodifiableList(result);
    }

    private GameplayEvent dropEvent(UUID playerUuid, AuraModel.AuraInstance aura) {
        Map<String, String> metadata = metadata(aura);
        metadata.put("rawRollCount", String.valueOf(aura.rawRolls.size()));
        metadata.put("aggregatedEffects", aggregateSummary(aura));
        return new GameplayEvent(playerUuid, GameplayEventType.AURA_DROPPED_FROM_CHEST, BigDecimal.ONE, metadata, "aura_chest");
    }

    private static GameplayEvent simpleInstanceEvent(UUID playerUuid, GameplayEventType type, AuraModel.AuraInstance aura) {
        return new GameplayEvent(playerUuid, type, BigDecimal.ONE, metadata(aura), "aura");
    }

    private static Map<String, String> metadata(AuraModel.AuraInstance aura) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("instanceId", aura.instanceId.toString()); metadata.put("rarity", aura.rarity.name()); metadata.put("origin", aura.origin.name());
        return metadata;
    }

    private static String aggregateSummary(AuraModel.AuraInstance aura) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<com.example.revolution.effect.EffectResolver.Stat, BigDecimal> entry : aura.aggregatedEffects.entrySet()) {
            if (out.length() > 0) out.append(','); out.append(entry.getKey().name()).append('=').append(entry.getValue().toPlainString());
        }
        return out.toString();
    }

    private List<AuraModel.AuraInstance> validatedConsumables(AuraModel.PlayerState state, AuraModel.Rarity rarity, List<UUID> ids) {
        Set<UUID> unique = new HashSet<>(ids); if (unique.size() != ids.size()) throw new IllegalArgumentException("duplicate Aura selection");
        List<AuraModel.AuraInstance> result = new ArrayList<>();
        for (UUID id : ids) { AuraModel.AuraInstance aura = state.find(id); if (aura == null || aura.rarity != rarity) throw new IllegalArgumentException("invalid Aura selection"); ensureConsumable(state, aura); result.add(aura); }
        return result;
    }

    private static void ensureConsumable(AuraModel.PlayerState state, AuraModel.AuraInstance aura) {
        if (aura.pinned) throw new IllegalStateException("pinned Aura cannot be consumed");
        if (aura.instanceId.equals(state.activeAuraId)) throw new IllegalStateException("active Aura cannot be consumed");
    }

    private void requireFeature() { if (!policy.featureEnabled()) throw new IllegalStateException("Aura System is locked"); }
    private long nextSeed() { synchronized (secureSeedSource) { return secureSeedSource.nextLong(); } }
    private static AuraModel.Rarity next(AuraModel.Rarity rarity) { AuraModel.Rarity[] values = AuraModel.Rarity.values(); return rarity == null || rarity.ordinal() + 1 >= values.length ? null : values[rarity.ordinal() + 1]; }
    private static AuraModel.Rarity previous(AuraModel.Rarity rarity) { return AuraModel.Rarity.values()[rarity.ordinal() - 1]; }
}
