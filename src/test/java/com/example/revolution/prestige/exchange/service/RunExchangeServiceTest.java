package com.example.revolution.prestige.exchange.service;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.pet.definition.PetCatalog;
import com.example.revolution.pet.repository.PetRepository;
import com.example.revolution.pet.service.PetBalanceService;
import com.example.revolution.pet.service.PetService;
import com.example.revolution.pet.state.PetPlayerState;
import com.example.revolution.prestige.PrestigeBranch;
import com.example.revolution.prestige.PrestigeBranchRegistry;
import com.example.revolution.prestige.PrestigeRepository;
import com.example.revolution.prestige.PrestigeService;
import com.example.revolution.prestige.PrestigeState;
import com.example.revolution.prestige.exchange.definition.RunExchangeCatalog;
import com.example.revolution.prestige.exchange.definition.RunExchangeOffer;
import com.example.revolution.resource.repository.ResourceRepository;
import com.example.revolution.resource.service.ResourceService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RunExchangeServiceTest {
    @Test public void baselineCatalogContainsExactlySevenApprovedOffers() {
        Fixture f = new Fixture();
        assertEquals(7, f.exchange.catalog().all().size());
        assertEquals(RunExchangeCatalog.baselineIds(), java.util.Arrays.asList(
                "pet_slot_fragments", "coins_fragments", "rune_dust_fragments", "shards_fragments",
                "coins_crystals", "rune_dust_crystals", "shards_crystals"));
        for (RunExchangeOffer offer : f.exchange.catalog().all()) {
            assertNotEquals("rune_cores", offer.targetResource);
            assertFalse(offer.offerId.contains("forge"));
            assertFalse(offer.offerId.contains("premium"));
        }
        assertEquals(RunExchangeOffer.PaymentCurrency.FRAGMENTS, f.exchange.catalog().get("pet_slot_fragments").paymentCurrency);
        assertEquals(RunExchangeOffer.PaymentCurrency.FRAGMENTS, f.exchange.catalog().get("coins_fragments").paymentCurrency);
        assertEquals(RunExchangeOffer.PaymentCurrency.CRYSTALS, f.exchange.catalog().get("coins_crystals").paymentCurrency);
    }

    @Test public void fragmentOfferUsesFactualBalanceAndIndependentIncreasingCounter() {
        Fixture f = new Fixture();
        f.setFragments("10");
        f.resourcesRepo.writePlayerResource(f.id, "coins", new BigDecimal("1000"));

        RunExchangeService.Preview first = f.exchange.preview(f.id, f.run.runId, "coins_fragments");
        assertNotNull(first);
        assertDecimal("1", first.currentCost);
        assertDecimal("10", first.rewardPercent);
        assertDecimal("100", first.expectedReward);
        assertEquals(0, first.purchaseCount);

        assertEquals(RunExchangeService.Result.APPLIED, f.exchange.exchange(f.id, f.run.runId, "coins_fragments"));
        assertDecimal("1100", f.resourcesRepo.getPlayerResourceValue(f.id, "coins"));
        assertDecimal("9", f.prestige.activeRun(f.id).runBranchCurrency);
        assertEquals(1, f.prestige.activeRun(f.id).runExchangePurchaseCounts.get("coins_fragments"));
        assertFalse(f.prestige.activeRun(f.id).runExchangePurchaseCounts.containsKey("coins_crystals"));

        RunExchangeService.Preview second = f.exchange.preview(f.id, f.run.runId, "coins_fragments");
        assertDecimal("1.35", second.currentCost);
        assertDecimal("110", second.expectedReward);
        assertEquals(1, second.purchaseCount);

        assertEquals(RunExchangeService.Result.APPLIED, f.exchange.exchange(f.id, f.run.runId, "coins_fragments"));
        assertDecimal("1210", f.resourcesRepo.getPlayerResourceValue(f.id, "coins"));
        assertDecimal("7.65", f.prestige.activeRun(f.id).runBranchCurrency);
        assertEquals(2, f.prestige.activeRun(f.id).runExchangePurchaseCounts.get("coins_fragments"));
    }

    @Test public void crystalOfferChargesCrystalsWithoutTouchingFragments() {
        Fixture f = new Fixture();
        f.setFragments("7");
        f.resourcesRepo.writePlayerResource(f.id, "crystals", new BigDecimal("100"));
        f.resourcesRepo.writePlayerResource(f.id, "rune_dust", new BigDecimal("1000"));

        assertEquals(RunExchangeService.Result.APPLIED, f.exchange.exchange(f.id, f.run.runId, "rune_dust_crystals"));
        assertDecimal("90", f.resourcesRepo.getPlayerResourceValue(f.id, "crystals"));
        assertDecimal("1100", f.resourcesRepo.getPlayerResourceValue(f.id, "rune_dust"));
        assertDecimal("7", f.prestige.activeRun(f.id).runBranchCurrency);
        assertEquals(1, f.prestige.activeRun(f.id).runExchangePurchaseCounts.get("rune_dust_crystals"));
        assertFalse(f.prestige.activeRun(f.id).runExchangePurchaseCounts.containsKey("rune_dust_fragments"));
    }

    @Test public void zeroTargetBalanceProducesZeroRewardButOnePurchase() {
        Fixture f = new Fixture();
        f.setFragments("2");
        assertEquals(RunExchangeService.Result.APPLIED, f.exchange.exchange(f.id, f.run.runId, "shards_fragments"));
        assertDecimal("0", f.resourcesRepo.getPlayerResourceValue(f.id, "rune_shards"));
        assertEquals(1, f.prestige.activeRun(f.id).runExchangePurchaseCounts.get("shards_fragments"));
        assertDecimal("1", f.prestige.activeRun(f.id).runBranchCurrency);
    }

    @Test public void staleAndInsufficientRequestsDoNotAdvanceCounters() {
        Fixture f = new Fixture();
        f.setFragments("0");
        f.resourcesRepo.writePlayerResource(f.id, "coins", new BigDecimal("1000"));
        assertEquals(RunExchangeService.Result.STALE_RUN, f.exchange.exchange(f.id, "old-run", "coins_fragments"));
        assertEquals(RunExchangeService.Result.INSUFFICIENT_FRAGMENTS, f.exchange.exchange(f.id, f.run.runId, "coins_fragments"));
        assertEquals(RunExchangeService.Result.INSUFFICIENT_CRYSTALS, f.exchange.exchange(f.id, f.run.runId, "coins_crystals"));
        assertTrue(f.prestige.activeRun(f.id).runExchangePurchaseCounts.isEmpty());
        assertDecimal("1000", f.resourcesRepo.getPlayerResourceValue(f.id, "coins"));
    }

    @Test public void petSlotOfferReusesCanonicalPetCounterAndCost() {
        Fixture f = new Fixture();
        f.setFragments("5");
        RunExchangeService.Preview preview = f.exchange.preview(f.id, f.run.runId, "pet_slot_fragments");
        assertNotNull(preview);
        assertDecimal("1", preview.currentCost);
        assertEquals(0, preview.purchaseCount);

        assertEquals(RunExchangeService.Result.APPLIED, f.exchange.exchange(f.id, f.run.runId, "pet_slot_fragments"));
        PetPlayerState petState = f.pets.snapshot(f.id);
        assertEquals(1, petState.run.normalSlotsUnlockedThisRun);
        assertEquals(1L, petState.run.totalNormalSlotPurchasesThisRun);
        assertFalse(f.prestige.activeRun(f.id).runExchangePurchaseCounts.containsKey("pet_slot_fragments"));
        assertDecimal("4", f.prestige.activeRun(f.id).runBranchCurrency);
    }

    @Test public void exchangeGrantDoesNotCreateFakeMasteryProgress() {
        Fixture f = new Fixture();
        f.setFragments("2");
        f.resourcesRepo.writePlayerResource(f.id, "coins", new BigDecimal("1000"));
        assertEquals(RunExchangeService.Result.APPLIED, f.exchange.exchange(f.id, f.run.runId, "coins_fragments"));
        PrestigeState.RunState after = f.prestige.activeRun(f.id);
        assertDecimal("0", after.branchProgress);
        assertDecimal("0", after.masteryPendingFragments);
        assertEquals(0, after.branchLevel);
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private static final class Fixture {
        final UUID id = UUID.randomUUID();
        final MemoryPrestigeRepo prestigeRepo = new MemoryPrestigeRepo();
        final MemoryResourceRepo resourcesRepo = new MemoryResourceRepo();
        final MemoryPetRepo petRepo = new MemoryPetRepo();
        final BalanceService balance = BalanceBootstrap.activateBundledDefaults();
        final PrestigeService prestige = new PrestigeService(prestigeRepo, new PrestigeBranchRegistry());
        final ResourceService resources = new ResourceService(resourcesRepo);
        final PetService pets = new PetService(petRepo, new PetCatalog(), new PetBalanceService(balance), resources, prestige);
        final PrestigeState.RunState run = prestige.startRun(id, PrestigeBranch.CLICK);
        final RunExchangeService exchange = new RunExchangeService(prestige, resources, pets, balance, RunExchangeCatalogFactory.fromBalance(balance));

        void setFragments(String amount) {
            PrestigeState state = prestigeRepo.load(id);
            state.activeRun.runBranchCurrency = new BigDecimal(amount);
            prestigeRepo.save(id, state);
        }
    }

    private static final class MemoryPrestigeRepo implements PrestigeRepository {
        private final Map<UUID, PrestigeState> states = new HashMap<>();
        public PrestigeState load(UUID id) { PrestigeState state = states.get(id); return state == null ? new PrestigeState() : state.copy(); }
        public void save(UUID id, PrestigeState state) { states.put(id, state.copy()); }
        public ResetCommitResult commitReset(UUID id, String expectedRunId, PrestigeState state) {
            states.put(id, state.copy()); return ResetCommitResult.APPLIED;
        }
    }

    private static final class MemoryResourceRepo implements ResourceRepository {
        private final Map<UUID, Map<String, BigDecimal>> values = new HashMap<>();
        public void purgeDeprecatedLightnings() {}
        public void purgeDeprecatedLightnings(UUID uuid) {}
        public BigDecimal getPlayerResourceValue(UUID uuid, String resource) {
            Map<String, BigDecimal> player = values.get(uuid);
            if (player == null) return BigDecimal.ZERO;
            BigDecimal value = player.get(resource);
            return value == null ? BigDecimal.ZERO : value;
        }
        public void writePlayerResource(UUID uuid, String canonical, BigDecimal value) {
            Map<String, BigDecimal> player = values.get(uuid);
            if (player == null) { player = new HashMap<>(); values.put(uuid, player); }
            player.put(canonical, value);
        }
    }

    private static final class MemoryPetRepo implements PetRepository {
        private final Map<UUID, PetPlayerState> states = new HashMap<>();
        public PetPlayerState load(UUID id) { PetPlayerState state = states.get(id); return state == null ? new PetPlayerState() : state.copy(); }
        public void save(UUID id, PetPlayerState state) { states.put(id, state.copy()); }
    }
}
