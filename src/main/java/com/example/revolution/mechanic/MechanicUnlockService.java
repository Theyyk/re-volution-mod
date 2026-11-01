package com.example.revolution.mechanic;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneLayoutDefinition;
import com.example.revolution.rune.repository.RuneRecord;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

/** Canonical server-authoritative access source for v2.12.0 mechanic unlocks. */
public final class MechanicUnlockService {
    public enum UnlockResult { OK, UNKNOWN, DISABLED, HIDDEN, NOT_ELIGIBLE, ALREADY_UNLOCKED }

    public static final class EntrySnapshot {
        public final MechanicUnlockModel.Definition definition;
        public final int progress;
        public final boolean visible;
        public final boolean eligible;
        public final boolean unlocked;
        EntrySnapshot(MechanicUnlockModel.Definition definition, int progress, boolean visible, boolean eligible, boolean unlocked) {
            this.definition = definition; this.progress = progress; this.visible = visible; this.eligible = eligible; this.unlocked = unlocked;
        }
    }

    private final PlayerRepository players;
    private final MechanicUnlockRepository repository;
    private final BalanceMechanicUnlockPolicy policy;

    public MechanicUnlockService(PlayerRepository players, MechanicUnlockRepository repository, BalanceMechanicUnlockPolicy policy) {
        if (players == null || repository == null || policy == null) throw new IllegalArgumentException("mechanic unlock dependencies are required");
        this.players = players; this.repository = repository; this.policy = policy;
    }

    public BalanceMechanicUnlockPolicy policy() { return policy; }

    /** Rebuilds factual Standard Rune counts and only raises historical maxima. */
    public MechanicUnlockModel.PlayerState refreshRuneProgress(UUID playerUuid) {
        MechanicUnlockModel.PlayerState state = repository.load(playerUuid);
        EnumMap<MechanicUnlockModel.Rarity, Integer> current = factualRuneProgress(playerUuid);
        boolean changed = false;
        for (MechanicUnlockModel.Rarity rarity : MechanicUnlockModel.Rarity.values()) {
            int factual = current.containsKey(rarity) ? current.get(rarity) : 0;
            int old = state.maxProgress(rarity);
            if (factual > old) { state.maxRuneProgressByRarity.put(rarity, factual); changed = true; }
        }
        if (changed) repository.save(playerUuid, state);
        return state;
    }

    public EnumMap<MechanicUnlockModel.Rarity, Integer> factualRuneProgress(UUID playerUuid) {
        EnumMap<MechanicUnlockModel.Rarity, Integer> result = new EnumMap<>(MechanicUnlockModel.Rarity.class);
        for (MechanicUnlockModel.Rarity rarity : MechanicUnlockModel.Rarity.values()) result.put(rarity, 0);
        for (RuneCollectionId collection : RuneCollectionId.values()) {
            int[] best = new int[RuneLayoutDefinition.SLOTS_PER_COLLECTION];
            for (RuneRecord rune : players.getRunesInCollection(playerUuid, collection.legacyIndex())) {
                if (rune == null || rune.runeIndex < 0 || rune.runeIndex >= best.length) continue;
                best[rune.runeIndex] = Math.max(best[rune.runeIndex], rune.layer);
            }
            for (int rank : best) {
                MechanicUnlockModel.Rarity rarity = rarity(rank);
                if (rarity != null) result.put(rarity, result.get(rarity) + 1);
            }
        }
        return result;
    }

    public boolean isUnlocked(UUID playerUuid, String mechanicId) {
        if (mechanicId == null) return false;
        MechanicUnlockModel.PlayerState state = repository.load(playerUuid);
        return isUnlocked(state, mechanicId);
    }

    public boolean isEligible(UUID playerUuid, String unlockId) {
        MechanicUnlockModel.Definition definition = policy.definition(unlockId);
        if (definition == null || !definition.enabled) return false;
        MechanicUnlockModel.PlayerState state = refreshRuneProgress(playerUuid);
        return visible(state, definition) && requirementMet(state, definition) && !isUnlocked(state, unlockId);
    }

    public UnlockResult unlock(UUID playerUuid, String unlockId) {
        MechanicUnlockModel.Definition definition = policy.definition(unlockId);
        if (definition == null) return UnlockResult.UNKNOWN;
        if (!definition.enabled) return UnlockResult.DISABLED;
        MechanicUnlockModel.PlayerState state = refreshRuneProgress(playerUuid);
        if (isUnlocked(state, unlockId)) return UnlockResult.ALREADY_UNLOCKED;
        if (!visible(state, definition)) return UnlockResult.HIDDEN;
        if (!requirementMet(state, definition)) return UnlockResult.NOT_ELIGIBLE;
        state.unlockedMechanicIds.add(unlockId);
        repository.save(playerUuid, state);
        return UnlockResult.OK;
    }

    public void forceUnlock(UUID playerUuid, String unlockId) {
        if (policy.definition(unlockId) == null) throw new IllegalArgumentException("unknown mechanic unlock '" + unlockId + "'");
        MechanicUnlockModel.PlayerState state = repository.load(playerUuid);
        state.forceLockedMechanicIds.remove(unlockId);
        state.forceUnlockedMechanicIds.add(unlockId);
        repository.save(playerUuid, state);
    }

    public void forceLock(UUID playerUuid, String unlockId) {
        if (policy.definition(unlockId) == null) throw new IllegalArgumentException("unknown mechanic unlock '" + unlockId + "'");
        MechanicUnlockModel.PlayerState state = repository.load(playerUuid);
        state.forceUnlockedMechanicIds.remove(unlockId);
        state.forceLockedMechanicIds.add(unlockId);
        repository.save(playerUuid, state);
    }

    public void clearOverride(UUID playerUuid, String unlockId) {
        MechanicUnlockModel.PlayerState state = repository.load(playerUuid);
        state.forceUnlockedMechanicIds.remove(unlockId);
        state.forceLockedMechanicIds.remove(unlockId);
        repository.save(playerUuid, state);
    }

    /** Explicit debug-only setter; normal gameplay never calls this. */
    public void setMaxProgress(UUID playerUuid, MechanicUnlockModel.Rarity rarity, int value) {
        MechanicUnlockModel.PlayerState state = repository.load(playerUuid);
        state.maxRuneProgressByRarity.put(rarity, Math.max(0, value));
        repository.save(playerUuid, state);
    }

    public MechanicUnlockModel.PlayerState state(UUID playerUuid) { return repository.load(playerUuid); }

    public List<EntrySnapshot> snapshot(UUID playerUuid) {
        MechanicUnlockModel.PlayerState state = refreshRuneProgress(playerUuid);
        List<EntrySnapshot> result = new ArrayList<>();
        for (MechanicUnlockModel.Definition definition : policy.all()) {
            if (!definition.enabled) continue;
            boolean visible = visible(state, definition);
            if (!visible) continue;
            boolean unlocked = isUnlocked(state, definition.unlockId);
            int progress = state.maxProgress(definition.rarity);
            result.add(new EntrySnapshot(definition, progress, true, !unlocked && requirementMet(state, definition), unlocked));
        }
        result.sort((a, b) -> {
            int ar = a.eligible && !a.unlocked ? 0 : a.unlocked ? 2 : 1;
            int br = b.eligible && !b.unlocked ? 0 : b.unlocked ? 2 : 1;
            if (ar != br) return Integer.compare(ar, br);
            return Integer.compare(a.definition.order, b.definition.order);
        });
        return result;
    }

    public boolean hasReadyVisibleEntry(UUID playerUuid) {
        for (EntrySnapshot entry : snapshot(playerUuid)) if (entry.eligible && !entry.unlocked) return true;
        return false;
    }

    private boolean visible(MechanicUnlockModel.PlayerState state, MechanicUnlockModel.Definition definition) {
        if (definition.visibility == MechanicUnlockModel.Visibility.VISIBLE_WHEN_LOCKED) return true;
        return !definition.parentUnlockId.isEmpty() && isUnlocked(state, definition.parentUnlockId);
    }

    private boolean requirementMet(MechanicUnlockModel.PlayerState state, MechanicUnlockModel.Definition definition) {
        if (definition.requirementType != MechanicUnlockModel.RequirementType.RUNE_PROGRESS_MAX) return false;
        return state.maxProgress(definition.rarity) >= definition.target;
    }

    private boolean isUnlocked(MechanicUnlockModel.PlayerState state, String mechanicId) {
        if (state.forceLockedMechanicIds.contains(mechanicId)) return false;
        if (state.forceUnlockedMechanicIds.contains(mechanicId)) return true;
        return state.unlockedMechanicIds.contains(mechanicId);
    }

    private static MechanicUnlockModel.Rarity rarity(int rank) {
        if (rank == RuneLayoutDefinition.ORDINARY_RANK) return MechanicUnlockModel.Rarity.COMMON;
        if (rank == RuneLayoutDefinition.IMPROVED_RANK) return MechanicUnlockModel.Rarity.IMPROVED;
        if (rank == RuneLayoutDefinition.ADVANCED_RANK) return MechanicUnlockModel.Rarity.ADVANCED;
        if (rank == RuneLayoutDefinition.MYTHIC_RANK) return MechanicUnlockModel.Rarity.MYTHIC;
        if (rank == RuneLayoutDefinition.ANCIENT_RANK) return MechanicUnlockModel.Rarity.ANCIENT;
        return null;
    }
}
