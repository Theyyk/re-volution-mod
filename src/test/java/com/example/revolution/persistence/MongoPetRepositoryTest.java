package com.example.revolution.persistence;

import com.example.revolution.pet.definition.PetRank;
import com.example.revolution.pet.state.PetPlayerState;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MongoPetRepositoryTest {
    @Test public void encodeDecodeRoundTripPreservesPermanentAndRunState() {
        PetPlayerState state = sampleState();
        PetPlayerState restored = MongoPetRepository.decode(MongoPetRepository.encode(state));

        assertEquals(2, restored.premiumPetSlotsUnlocked);
        assertEquals(PetRank.ADVANCED, restored.pets.get("pet_03").rank);
        assertEquals(BigInteger.valueOf(9L), restored.pets.get("pet_03").level);
        assertEquals(0, new BigDecimal("77.25").compareTo(restored.pets.get("pet_03").xp));
        assertEquals("run-1", restored.run.runId);
        assertEquals(5, restored.run.normalSlotsUnlockedThisRun);
        assertEquals(6L, restored.run.totalNormalSlotPurchasesThisRun);
        assertEquals(4, restored.run.selectedPets.size());
        assertTrue(restored.run.purchasedPetIds.contains("pet_01"));
        assertEquals(123456789L, restored.run.cooldownUntil.get("pet_02").longValue());
        assertEquals(999L, restored.run.lastPetXpAccrualAt);
    }

    @Test public void prestigeWipesLowerRanksAndRunStateWhilePreservingAdvancedPlus() {
        PetPlayerState state = sampleState();
        Document reset = MongoPetRepository.resetForPrestige(
                new Document("petSystem", MongoPetRepository.encode(state)));
        PetPlayerState restored = MongoPetRepository.decode(reset);

        assertEquals(2, restored.premiumPetSlotsUnlocked);
        assertFalse(restored.pets.containsKey("pet_01"));
        assertFalse(restored.pets.containsKey("pet_02"));
        assertEquals(PetRank.ADVANCED, restored.pets.get("pet_03").rank);
        assertEquals(BigInteger.valueOf(9L), restored.pets.get("pet_03").level);
        assertEquals(0, new BigDecimal("77.25").compareTo(restored.pets.get("pet_03").xp));
        assertEquals(PetRank.MYTHIC, restored.pets.get("pet_04").rank);

        assertNotNull(restored.run);
        assertNull(restored.run.runId);
        assertEquals(0, restored.run.normalSlotsUnlockedThisRun);
        assertEquals(0L, restored.run.totalNormalSlotPurchasesThisRun);
        assertTrue(restored.run.purchasedPetIds.isEmpty());
        assertTrue(restored.run.cooldownUntil.isEmpty());
        assertEquals(0L, restored.run.lastPetXpAccrualAt);
        assertEquals(2, restored.run.selectedPets.size());
        assertEquals("pet_04", restored.run.selectedPets.get(0).petId);
        assertEquals("pet_03", restored.run.selectedPets.get(1).petId);
    }

    private static PetPlayerState sampleState() {
        PetPlayerState state = new PetPlayerState();
        state.premiumPetSlotsUnlocked = 2;
        progress(state, "pet_01", PetRank.ORDINARY, 5, "12");
        progress(state, "pet_02", PetRank.IMPROVED, 7, "33");
        progress(state, "pet_03", PetRank.ADVANCED, 9, "77.25");
        progress(state, "pet_04", PetRank.MYTHIC, 11, "120");

        PetPlayerState.RunState run = new PetPlayerState.RunState();
        run.runId = "run-1";
        run.purchasedPetIds.add("pet_01");
        run.purchasedPetIds.add("pet_02");
        run.normalSlotsUnlockedThisRun = 5;
        run.totalNormalSlotPurchasesThisRun = 6L;
        run.lastPetXpAccrualAt = 999L;
        run.cooldownUntil.put("pet_02", 123456789L);
        run.selectedPets.add(new PetPlayerState.SelectedPet("pet_01", false, 1L));
        run.selectedPets.add(new PetPlayerState.SelectedPet("pet_02", false, 2L));
        run.selectedPets.add(new PetPlayerState.SelectedPet("pet_03", false, 3L));
        run.selectedPets.add(new PetPlayerState.SelectedPet("pet_04", true, 4L));
        state.run = run;
        return state;
    }

    private static void progress(PetPlayerState state, String id, PetRank rank, long level, String xp) {
        PetPlayerState.PetProgress progress = state.pet(id);
        progress.rank = rank;
        progress.level = BigInteger.valueOf(level);
        progress.xp = new BigDecimal(xp);
    }
}
