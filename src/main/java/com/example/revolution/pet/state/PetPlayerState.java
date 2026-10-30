package com.example.revolution.pet.state;

import com.example.revolution.pet.definition.PetRank;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Persistence-neutral Pet state split into permanent records and one run-scoped layer. */
public final class PetPlayerState {
    public int premiumPetSlotsUnlocked;
    public final Map<String, PetProgress> pets = new LinkedHashMap<>();
    public RunState run;

    public PetProgress pet(String petId) {
        PetProgress existing = pets.get(petId);
        if (existing != null) return existing;
        PetProgress created = new PetProgress();
        pets.put(petId, created);
        return created;
    }

    public PetPlayerState copy() {
        PetPlayerState copy = new PetPlayerState();
        copy.premiumPetSlotsUnlocked = premiumPetSlotsUnlocked;
        for (Map.Entry<String, PetProgress> entry : pets.entrySet()) copy.pets.put(entry.getKey(), entry.getValue().copy());
        copy.run = run == null ? null : run.copy();
        return copy;
    }

    public static final class PetProgress {
        public PetRank rank = PetRank.ORDINARY;
        public BigInteger level = BigInteger.ONE;
        public BigDecimal xp = BigDecimal.ZERO;

        public PetProgress copy() {
            PetProgress copy = new PetProgress();
            copy.rank = rank;
            copy.level = level;
            copy.xp = xp;
            return copy;
        }
    }

    public static final class RunState {
        public String runId;
        public final Set<String> purchasedPetIds = new LinkedHashSet<>();
        /** Compact board: indices 0..normal+premium-1. Null values are never persisted. */
        public final List<SelectedPet> selectedPets = new ArrayList<>();
        public final Map<String, Long> cooldownUntil = new LinkedHashMap<>();
        public int normalSlotsUnlockedThisRun;
        public long totalNormalSlotPurchasesThisRun;
        public long lastPetXpAccrualAt;

        public RunState copy() {
            RunState copy = new RunState();
            copy.runId = runId;
            copy.purchasedPetIds.addAll(purchasedPetIds);
            for (SelectedPet selected : selectedPets) copy.selectedPets.add(selected.copy());
            copy.cooldownUntil.putAll(cooldownUntil);
            copy.normalSlotsUnlockedThisRun = normalSlotsUnlockedThisRun;
            copy.totalNormalSlotPurchasesThisRun = totalNormalSlotPurchasesThisRun;
            copy.lastPetXpAccrualAt = lastPetXpAccrualAt;
            return copy;
        }
    }

    public static final class SelectedPet {
        public String petId;
        /** True only for the physical premium slot occupied by this selection. */
        public boolean premiumSlot;
        /** Stable ordering key assigned on first selection/awakening normalization. */
        public long order;

        public SelectedPet() {}

        public SelectedPet(String petId, boolean premiumSlot, long order) {
            this.petId = petId;
            this.premiumSlot = premiumSlot;
            this.order = order;
        }

        public SelectedPet copy() { return new SelectedPet(petId, premiumSlot, order); }
    }
}
