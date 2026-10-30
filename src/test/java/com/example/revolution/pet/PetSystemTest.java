package com.example.revolution.pet;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.effect.EffectResolver;
import com.example.revolution.pet.definition.PetCatalog;
import com.example.revolution.pet.definition.PetRank;
import com.example.revolution.pet.repository.PetRepository;
import com.example.revolution.pet.service.PetBalanceService;
import com.example.revolution.pet.service.PetEffectSource;
import com.example.revolution.pet.service.PetService;
import com.example.revolution.pet.state.PetPlayerState;
import com.example.revolution.prestige.PrestigeBranch;
import com.example.revolution.prestige.PrestigeBranchRegistry;
import com.example.revolution.prestige.PrestigeRepository;
import com.example.revolution.prestige.PrestigeService;
import com.example.revolution.prestige.PrestigeState;
import com.example.revolution.resource.repository.ResourceRepository;
import com.example.revolution.resource.service.ResourceService;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PetSystemTest {
    @Test public void purchaseIsOncePerRunLowerRankRepurchasesAndAdvancedOwnershipPersists() {
        Fixture f = new Fixture(); f.resources.put("coins", new BigDecimal("100000"));
        assertEquals(PetService.Result.APPLIED, f.pets.purchasePet(f.id, "pet_01")); BigDecimal after = f.resources.get("coins");
        assertEquals(PetService.Result.ALREADY_OWNED, f.pets.purchasePet(f.id, "pet_01")); assertEquals(after, f.resources.get("coins"));
        f.startNewRun(); assertEquals(PetService.Result.APPLIED, f.pets.purchasePet(f.id, "pet_01"));
        PetPlayerState state = f.petRepo.load(f.id); state.pet("pet_01").rank = PetRank.ADVANCED; f.petRepo.save(f.id, state); f.startNewRun();
        assertEquals(PetService.Result.ALREADY_OWNED, f.pets.purchasePet(f.id, "pet_01"));
    }

    @Test public void normalSlotPriceIsContinuousThroughAwakeningAndResetsWithRun() {
        Fixture f = funded(); BigDecimal first = f.pets.nextNormalSlotCost(f.id); assertEquals(PetService.Result.APPLIED, f.pets.purchaseNormalSlot(f.id));
        PetPlayerState afterPurchase = f.pets.snapshot(f.id); assertEquals(1L, afterPurchase.run.totalNormalSlotPurchasesThisRun);
        BigDecimal second = f.pets.nextNormalSlotCost(f.id); assertTrue(second.compareTo(first) >= 0); buyAndSelect(f, "pet_01");
        assertEquals(PetService.Result.APPLIED, f.pets.awaken(f.id, "pet_01")); assertEquals(1L, f.pets.snapshot(f.id).run.totalNormalSlotPurchasesThisRun);
        assertEquals(0, second.compareTo(f.pets.nextNormalSlotCost(f.id))); f.startNewRun(); PetPlayerState snapshot = f.pets.snapshot(f.id);
        assertEquals(0, snapshot.run.normalSlotsUnlockedThisRun); assertEquals(0L, snapshot.run.totalNormalSlotPurchasesThisRun); assertEquals(0, first.compareTo(f.pets.nextNormalSlotCost(f.id)));
    }

    @Test public void premiumEntitlementPersistsAcrossRunsAndSelectionCannotDuplicate() {
        Fixture f = funded(); assertEquals(PetService.Result.APPLIED, f.pets.purchasePremiumSlot(f.id)); assertEquals(PetService.Result.APPLIED, f.pets.purchaseNormalSlot(f.id));
        buyAndSelect(f, "pet_01"); assertEquals(PetService.Result.SELECTED, f.pets.selectPet(f.id, "pet_01")); f.startNewRun(); assertEquals(1, f.pets.snapshot(f.id).premiumPetSlotsUnlocked);
    }

    @Test public void awakeningRequiresFullBoardAndPremiumPetSkipsCooldown() {
        Fixture f = funded(); assertEquals(PetService.Result.APPLIED, f.pets.purchaseNormalSlot(f.id)); assertEquals(PetService.Result.APPLIED, f.pets.purchasePremiumSlot(f.id));
        f.pets.purchasePet(f.id, "pet_01"); f.pets.purchasePet(f.id, "pet_02"); assertEquals(PetService.Result.APPLIED, f.pets.selectPet(f.id, "pet_01"));
        assertEquals(PetService.Result.BOARD_NOT_FULL, f.pets.awaken(f.id, "pet_01")); assertEquals(PetService.Result.APPLIED, f.pets.selectPet(f.id, "pet_02"));
        PetPlayerState before = f.petRepo.load(f.id); before.pet("pet_01").level = BigInteger.TEN; before.pet("pet_01").xp = new BigDecimal("7.5"); f.petRepo.save(f.id, before);
        assertEquals(PetService.Result.APPLIED, f.pets.awaken(f.id, "pet_01")); PetPlayerState state = f.pets.snapshot(f.id);
        assertEquals(PetRank.IMPROVED, state.pets.get("pet_01").rank); assertEquals(BigInteger.TEN, state.pets.get("pet_01").level);
        assertEquals(0, new BigDecimal("7.5").compareTo(state.pets.get("pet_01").xp)); assertFalse(state.run.cooldownUntil.containsKey("pet_02"));
        assertEquals(1, state.run.selectedPets.size()); assertEquals("pet_01", state.run.selectedPets.get(0).petId);
    }

    @Test public void awakeningCooldownsNormalPetsAndDeterministicallyPreservesHigherRank() {
        Fixture f = funded(); f.pets.purchaseNormalSlot(f.id); f.pets.purchaseNormalSlot(f.id); f.pets.purchaseNormalSlot(f.id);
        buyAndSelect(f, "pet_01"); buyAndSelect(f, "pet_02"); buyAndSelect(f, "pet_03"); PetPlayerState before = f.petRepo.load(f.id);
        before.pet("pet_02").rank = PetRank.ADVANCED; before.pet("pet_03").level = BigInteger.valueOf(9L); before.pet("pet_03").xp = BigDecimal.TEN; f.petRepo.save(f.id, before);
        assertEquals(PetService.Result.APPLIED, f.pets.awaken(f.id, "pet_01")); PetPlayerState after = f.pets.snapshot(f.id);
        assertEquals(2, after.run.selectedPets.size()); assertEquals("pet_02", after.run.selectedPets.get(0).petId); assertEquals("pet_01", after.run.selectedPets.get(1).petId);
        assertTrue(after.run.cooldownUntil.get("pet_03") > System.currentTimeMillis()); assertEquals(BigInteger.ONE, after.pets.get("pet_03").level);
        assertEquals(0, BigDecimal.ZERO.compareTo(after.pets.get("pet_03").xp)); assertEquals(PetService.Result.COOLDOWN, f.pets.selectPet(f.id, "pet_03"));
    }

    @Test public void expiredCooldownUsesRealTimeAndAllowsSelectionAgain() {
        Fixture f = funded(); f.pets.purchaseNormalSlot(f.id); f.pets.purchasePet(f.id, "pet_01"); PetPlayerState state = f.pets.snapshot(f.id);
        state.run.cooldownUntil.put("pet_01", System.currentTimeMillis() - 1L); f.petRepo.save(f.id, state); assertEquals(PetService.Result.APPLIED, f.pets.selectPet(f.id, "pet_01"));
    }

    @Test public void xpAccumulatesBeyondThresholdWithoutAutomaticLevelAndMultiplierStillApplies() {
        Fixture f = funded(); f.pets.purchaseNormalSlot(f.id); f.pets.purchaseNormalSlot(f.id); buyAndSelect(f, "pet_09"); buyAndSelect(f, "pet_01");
        assertEquals(PetService.Result.APPLIED, f.pets.purchasePet(f.id, "pet_02")); PetEffectSource source = new PetEffectSource(f.petRepo, f.catalog, f.petBalance);
        EffectResolver resolver = new EffectResolver(); resolver.register(source); BigDecimal multiplier = resolver.multiplier(f.id, EffectResolver.Stat.PET_XP_MULTIPLIER);
        assertTrue(multiplier.compareTo(BigDecimal.ONE) > 0); f.pets.accrueOnlineXp(f.id, 200000L, multiplier); PetPlayerState state = f.pets.snapshot(f.id);
        assertEquals(BigInteger.ONE, state.pets.get("pet_09").level); assertEquals(BigInteger.ONE, state.pets.get("pet_01").level);
        assertTrue(state.pets.get("pet_09").xp.compareTo(f.petBalance.xpForNextLevel(BigInteger.ONE)) > 0);
        assertTrue(state.pets.get("pet_01").xp.compareTo(f.petBalance.xpForNextLevel(BigInteger.ONE)) > 0);
        assertEquals(BigInteger.ONE, state.pets.get("pet_02").level); assertEquals(0, BigDecimal.ZERO.compareTo(state.pets.get("pet_02").xp));
    }

    @Test public void manualLevelUpAppliesExactlyOneLevelPreservesXpAndAwardsPending() {
        Fixture f = funded(); f.pets.purchaseNormalSlot(f.id); buyAndSelect(f, "pet_01");
        PetPlayerState state = f.petRepo.load(f.id); state.pet("pet_01").xp = new BigDecimal("1000"); f.petRepo.save(f.id, state);
        String runId = f.prestige.activeRun(f.id).runId; BigDecimal beforeXp = f.pets.snapshot(f.id).pets.get("pet_01").xp;
        assertEquals(PetService.Result.APPLIED, f.pets.levelUpPet(f.id, "pet_01", runId));
        PetPlayerState after = f.pets.snapshot(f.id); assertEquals(BigInteger.valueOf(2), after.pets.get("pet_01").level); assertEquals(0, beforeXp.compareTo(after.pets.get("pet_01").xp));
        PrestigeState.RunState run = f.prestige.activeRun(f.id); assertEquals(0, f.petBalance.levelUpFragmentReward().compareTo(run.petPendingFragments));
        assertEquals(0, new BigDecimal("1000000").compareTo(run.runBranchCurrency));
        assertEquals(PetService.Result.APPLIED, f.pets.levelUpPet(f.id, "pet_01", runId)); assertEquals(BigInteger.valueOf(3), f.pets.snapshot(f.id).pets.get("pet_01").level);
    }

    @Test public void manualLevelUpRejectsInsufficientXpInactivePetAndStaleRun() {
        Fixture f = funded(); f.pets.purchaseNormalSlot(f.id); f.pets.purchasePet(f.id, "pet_01"); String runId = f.prestige.activeRun(f.id).runId;
        assertEquals(PetService.Result.NOT_ELIGIBLE, f.pets.levelUpPet(f.id, "pet_01", runId));
        assertEquals(PetService.Result.APPLIED, f.pets.selectPet(f.id, "pet_01")); assertEquals(PetService.Result.STALE_RUN, f.pets.levelUpPet(f.id, "pet_01", "stale"));
        assertEquals(PetService.Result.NOT_ELIGIBLE, f.pets.levelUpPet(f.id, "pet_01", runId));
    }

    @Test public void petCollectMovesOnlyPetPendingIntoSharedFragments() {
        Fixture f = funded(); PrestigeState state = f.prestigeRepo.load(f.id); state.activeRun.masteryPendingFragments = new BigDecimal("10"); state.activeRun.petPendingFragments = new BigDecimal("20"); state.activeRun.runBranchCurrency = new BigDecimal("40"); f.prestigeRepo.save(f.id, state);
        String runId = state.activeRun.runId; assertEquals(PetService.Result.APPLIED, f.pets.collectPetFragments(f.id, runId));
        PrestigeState.RunState run = f.prestige.activeRun(f.id); assertEquals(0, new BigDecimal("60").compareTo(run.runBranchCurrency)); assertEquals(0, new BigDecimal("10").compareTo(run.masteryPendingFragments)); assertEquals(0, BigDecimal.ZERO.compareTo(run.petPendingFragments));
        assertEquals(PetService.Result.APPLIED, f.pets.collectPetFragments(f.id, runId)); assertEquals(0, new BigDecimal("60").compareTo(f.prestige.activeRun(f.id).runBranchCurrency));
    }

    @Test public void effectStackingTradeoffExactZeroAndAutoclickRateAreResolvedFromSelectedPets() {
        Fixture f = funded(); f.pets.purchaseNormalSlot(f.id); f.pets.purchaseNormalSlot(f.id); f.pets.purchaseNormalSlot(f.id); buyAndSelect(f, "pet_13"); buyAndSelect(f, "pet_06"); buyAndSelect(f, "pet_11");
        PetEffectSource source = new PetEffectSource(f.petRepo, f.catalog, f.petBalance); EffectResolver resolver = new EffectResolver(); resolver.register(source);
        assertEquals(0, resolver.multiplier(f.id, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER).compareTo(BigDecimal.ZERO)); assertTrue(resolver.multiplier(f.id, EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER).compareTo(BigDecimal.ONE) > 0); assertTrue(source.autoClickCps(f.id).signum() > 0);
    }

    @Test public void rankRevertIsFreeAndRecomputesRunOwnership() {
        Fixture f = funded(); f.pets.purchaseNormalSlot(f.id); buyAndSelect(f, "pet_13"); BigDecimal coins = f.resources.get("coins");
        PetPlayerState state = f.petRepo.load(f.id); state.pet("pet_13").rank = PetRank.MYTHIC; f.petRepo.save(f.id, state);
        assertEquals(PetService.Result.APPLIED, f.pets.revertRank(f.id, "pet_13")); assertEquals(PetRank.ADVANCED, f.pets.snapshot(f.id).pets.get("pet_13").rank); assertEquals(coins, f.resources.get("coins"));
        assertEquals(PetService.Result.APPLIED, f.pets.revertRank(f.id, "pet_13")); PetPlayerState reverted = f.pets.snapshot(f.id); assertEquals(PetRank.IMPROVED, reverted.pets.get("pet_13").rank); assertTrue(reverted.run.purchasedPetIds.contains("pet_13"));
    }

    private static Fixture funded() { Fixture f = new Fixture(); f.resources.put("coins", new BigDecimal("1000000")); f.resources.put("crystals", new BigDecimal("1000000")); f.fragments(new BigDecimal("1000000")); return f; }
    private static void buyAndSelect(Fixture f, String petId) { assertEquals(PetService.Result.APPLIED, f.pets.purchasePet(f.id, petId)); assertEquals(PetService.Result.APPLIED, f.pets.selectPet(f.id, petId)); }

    private static final class Fixture {
        final UUID id = UUID.randomUUID(); final MemoryPetRepo petRepo = new MemoryPetRepo(); final MemoryResourceRepo resources = new MemoryResourceRepo(id); final MemoryPrestigeRepo prestigeRepo = new MemoryPrestigeRepo();
        final PrestigeService prestige = new PrestigeService(prestigeRepo, new PrestigeBranchRegistry()); final PetCatalog catalog = new PetCatalog();
        final PetBalanceService petBalance = new PetBalanceService(BalanceBootstrap.activateBundledDefaults()); final PetService pets = new PetService(petRepo, catalog, petBalance, new ResourceService(resources), prestige);
        Fixture() { prestige.startRun(id, PrestigeBranch.CLICK); }
        void fragments(BigDecimal value) { PrestigeState s = prestigeRepo.load(id); s.activeRun.runBranchCurrency = value; prestigeRepo.save(id, s); }
        void startNewRun() { PrestigeState s = prestigeRepo.load(id); s.activeRun = null; prestigeRepo.save(id, s); prestige.startRun(id, PrestigeBranch.CLICK); }
    }
    private static final class MemoryPetRepo implements PetRepository {
        private final Map<UUID, PetPlayerState> values = new HashMap<>(); public PetPlayerState load(UUID id) { PetPlayerState s = values.get(id); return s == null ? new PetPlayerState() : s.copy(); } public void save(UUID id, PetPlayerState state) { values.put(id, state.copy()); }
    }
    private static final class MemoryResourceRepo implements ResourceRepository {
        private final UUID id; private final Map<String, BigDecimal> values = new HashMap<>(); MemoryResourceRepo(UUID id) { this.id = id; } void put(String key, BigDecimal value) { values.put(key, value); } BigDecimal get(String key) { return getPlayerResourceValue(id, key); }
        public void purgeDeprecatedLightnings() {} public void purgeDeprecatedLightnings(UUID uuid) {} public BigDecimal getPlayerResourceValue(UUID uuid, String resource) { BigDecimal v = values.get(resource); return v == null ? BigDecimal.ZERO : v; } public void writePlayerResource(UUID uuid, String canonical, BigDecimal value) { values.put(canonical, value); }
    }
    private static final class MemoryPrestigeRepo implements PrestigeRepository {
        private final Map<UUID, PrestigeState> values = new HashMap<>(); public PrestigeState load(UUID id) { PrestigeState s = values.get(id); return s == null ? new PrestigeState() : s.copy(); } public void save(UUID id, PrestigeState state) { values.put(id, state.copy()); } public ResetCommitResult commitReset(UUID id, String run, PrestigeState state) { values.put(id, state.copy()); return ResetCommitResult.APPLIED; }
    }
}
