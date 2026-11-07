package com.example.revolution.unique;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.resource.repository.ResourceRepository;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.unique.UniqueRuneModel.CollectionId;
import com.example.revolution.unique.UniqueRuneModel.PlayerState;
import com.example.revolution.unique.UniqueRuneModel.Rarity;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class UniqueRuneServiceTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000211");

    @Test void crystalDuplicateGrantsPearlOnceAndReplayCannotDoubleSpend() {
        Fixture f = new Fixture();
        f.resources.setPlayerResourceValue(PLAYER, "crystals", new BigDecimal("1000"));
        UniqueRuneService.Result first = f.service.purchaseCrystal(PLAYER, 1L);
        assertEquals(UniqueRuneService.ResultCode.OK, first.code);
        BigDecimal afterFirst = f.resources.getPlayerResourceValue(PLAYER, "crystals");

        UniqueRuneService.Result duplicate = f.service.purchaseCrystal(PLAYER, 2L);
        assertEquals(UniqueRuneService.ResultCode.DUPLICATE, duplicate.code);
        assertEquals(f.policy.duplicatePearl(), f.resources.getPlayerResourceValue(PLAYER, "pearl"));
        BigDecimal afterDuplicate = f.resources.getPlayerResourceValue(PLAYER, "crystals");
        assertTrue(afterDuplicate.compareTo(afterFirst) < 0);

        assertEquals(UniqueRuneService.ResultCode.REPLAYED, f.service.purchaseCrystal(PLAYER, 2L).code);
        assertEquals(afterDuplicate, f.resources.getPlayerResourceValue(PLAYER, "crystals"));
        assertEquals(f.policy.duplicatePearl(), f.resources.getPlayerResourceValue(PLAYER, "pearl"));
    }

    @Test void crystalPurchaseUsesFullPoolAndControlledRandomRoll() {
        Fixture first = new Fixture();
        Fixture last = new Fixture(true);
        first.resources.setPlayerResourceValue(PLAYER, "crystals", new BigDecimal("1000"));
        last.resources.setPlayerResourceValue(PLAYER, "crystals", new BigDecimal("1000"));
        UniqueRuneService.Result low = first.service.purchaseCrystal(PLAYER, 1L);
        UniqueRuneService.Result high = last.service.purchaseCrystal(PLAYER, 1L);
        assertEquals("crystal_01", low.runeId);
        assertEquals("crystal_26", high.runeId);
        assertEquals(1, first.service.snapshot(PLAYER).collection(CollectionId.CRYSTAL_COLLECTION).progress());
        assertEquals(1, last.service.snapshot(PLAYER).collection(CollectionId.CRYSTAL_COLLECTION).progress());
        assertEquals("crystal_01", first.service.purchaseCrystal(PLAYER, 2L).runeId);
        assertEquals("crystal_26", last.service.purchaseCrystal(PLAYER, 2L).runeId);
        assertEquals(1, first.service.snapshot(PLAYER).collection(CollectionId.CRYSTAL_COLLECTION).progress());
        assertEquals(1, last.service.snapshot(PLAYER).collection(CollectionId.CRYSTAL_COLLECTION).progress());
    }

    @Test void crystalPurchaseRejectsInsufficientBalanceWithoutGrantingOwnership() {
        Fixture f = new Fixture();
        assertEquals(UniqueRuneService.ResultCode.INSUFFICIENT_RESOURCE,
                f.service.purchaseCrystal(PLAYER, 1L).code);
        assertEquals(0, f.service.snapshot(PLAYER).collection(CollectionId.CRYSTAL_COLLECTION).progress());
        assertEquals(BigDecimal.ZERO, f.resources.getPlayerResourceValue(PLAYER, "pearl"));
    }

    @Test void crystalEnhancementUsesFixedPearlPerSuccessfulOperation() {
        Fixture f = new Fixture();
        PlayerState state = f.unique.load(PLAYER);
        state.collection(CollectionId.CRYSTAL_COLLECTION).owned.put("crystal_01", Rarity.COMMON);
        state.collection(CollectionId.CRYSTAL_COLLECTION).enhancement.put("crystal_01", 30);
        f.unique.save(PLAYER, state);
        f.resources.setPlayerResourceValue(PLAYER, "pearl", new BigDecimal("2"));
        assertEquals(UniqueRuneService.ResultCode.OK, f.service.enhance(PLAYER, 1L).code);
        assertEquals(0, BigDecimal.ONE.compareTo(f.resources.getPlayerResourceValue(PLAYER, "pearl")));
        assertEquals(31, f.service.snapshot(PLAYER).collection(CollectionId.CRYSTAL_COLLECTION).enhancement("crystal_01"));
        assertEquals(UniqueRuneService.ResultCode.OK, f.service.enhance(PLAYER, 2L).code);
        assertEquals(0, BigDecimal.ZERO.compareTo(f.resources.getPlayerResourceValue(PLAYER, "pearl")));
        assertEquals(UniqueRuneService.ResultCode.INSUFFICIENT_RESOURCE, f.service.enhance(PLAYER, 3L).code);
        assertEquals(32, f.service.snapshot(PLAYER).collection(CollectionId.CRYSTAL_COLLECTION).enhancement("crystal_01"));
    }

    @Test void dailyExcludesOwnedRuneAndAllowsOnlyOneClaimPerDay() {
        Fixture f = new Fixture();
        UniqueRuneService.Result first = f.service.claimDaily(PLAYER, 100L, 1L);
        assertEquals(UniqueRuneService.ResultCode.OK, first.code);
        assertEquals(UniqueRuneService.ResultCode.DAILY_ALREADY_CLAIMED, f.service.claimDaily(PLAYER, 100L, 2L).code);
        UniqueRuneService.Result secondDay = f.service.claimDaily(PLAYER, 101L, 3L);
        assertEquals(UniqueRuneService.ResultCode.OK, secondDay.code);
        assertNotEquals(first.runeId, secondDay.runeId);
        assertEquals(2, f.unique.load(PLAYER).collection(CollectionId.DAILY_COLLECTION).progress());
    }

    @Test void dailyCannotClaimAgainWhenDayMovesBackwards() {
        Fixture f = new Fixture();
        assertEquals(UniqueRuneService.ResultCode.OK, f.service.claimDaily(PLAYER, 220L, 1L).code);
        int before = f.unique.load(PLAYER).collection(CollectionId.DAILY_COLLECTION).progress();
        assertEquals(UniqueRuneService.ResultCode.DAILY_ALREADY_CLAIMED,
                f.service.claimDaily(PLAYER, 219L, 2L).code);
        assertEquals(UniqueRuneService.ResultCode.DAILY_ALREADY_CLAIMED,
                f.service.claimDaily(PLAYER, 220L, 3L).code);
        assertEquals(before, f.unique.load(PLAYER).collection(CollectionId.DAILY_COLLECTION).progress());
        assertEquals(220L, f.unique.load(PLAYER).lastDailyClaimDay);
        assertEquals(UniqueRuneService.ResultCode.OK,
                f.service.claimDaily(PLAYER, 221L, 4L).code);
    }

    @Test void offeringsDuplicateHasNoPearlCompensation() {
        Fixture f = new Fixture();
        f.offerings = new BigDecimal("1000");
        assertEquals(UniqueRuneService.ResultCode.OK, f.service.purchaseOfferings(PLAYER, 1L).code);
        BigDecimal afterFirst = f.offerings;
        UniqueRuneService.Result duplicate = f.service.purchaseOfferings(PLAYER, 2L);
        assertEquals(UniqueRuneService.ResultCode.DUPLICATE, duplicate.code);
        assertEquals(BigDecimal.ZERO, duplicate.compensation);
        assertEquals(BigDecimal.ZERO, f.resources.getPlayerResourceValue(PLAYER, "pearl"));
        assertTrue(f.offerings.compareTo(afterFirst) < 0);
    }

    @Test void awakeningAndRollbackAffectOnlySelectedRuneAcrossMixedRarities() {
        Fixture f = new Fixture();
        assertEquals(UniqueRuneService.ResultCode.INVALID,
                f.service.awaken(PLAYER, CollectionId.CRYSTAL_COLLECTION, "crystal_01", 1L).code);
        PlayerState state = f.unique.load(PLAYER);
        for (UniqueRuneModel.RuneDefinition rune : f.policy.definition(CollectionId.CRYSTAL_COLLECTION).runes)
            state.collection(CollectionId.CRYSTAL_COLLECTION).owned.put(rune.runeId, Rarity.COMMON);
        state.collection(CollectionId.CRYSTAL_COLLECTION).enhancement.put("crystal_01", 7);
        state.collection(CollectionId.CRYSTAL_COLLECTION).enhancement.put("crystal_02", 5);
        f.unique.save(PLAYER, state);

        assertEquals(UniqueRuneService.ResultCode.OK,
                f.service.awaken(PLAYER, CollectionId.CRYSTAL_COLLECTION, "crystal_01", 2L).code);
        PlayerState awakened = f.unique.load(PLAYER);
        assertEquals(26, awakened.collection(CollectionId.CRYSTAL_COLLECTION).progress());
        assertEquals(Rarity.IMPROVED, awakened.collection(CollectionId.CRYSTAL_COLLECTION).owned.get("crystal_01"));
        assertEquals(Rarity.COMMON, awakened.collection(CollectionId.CRYSTAL_COLLECTION).owned.get("crystal_02"));
        assertEquals(5, awakened.collection(CollectionId.CRYSTAL_COLLECTION).enhancement("crystal_02"));
        assertTrue(awakened.collection(CollectionId.CRYSTAL_COLLECTION).canRollback("crystal_01"));
        assertFalse(awakened.collection(CollectionId.CRYSTAL_COLLECTION).canRollback("crystal_02"));
        assertEquals(UniqueRuneService.ResultCode.NO_ROLLBACK,
                f.service.rollback(PLAYER, CollectionId.CRYSTAL_COLLECTION, "crystal_02", true, 3L).code);
        assertEquals(UniqueRuneService.ResultCode.CONFIRMATION_REQUIRED,
                f.service.rollback(PLAYER, CollectionId.CRYSTAL_COLLECTION, "crystal_01", false, 4L).code);
        f.resources.setPlayerResourceValue(PLAYER, "crystals", new BigDecimal("1000"));
        assertEquals(UniqueRuneService.ResultCode.OK,
                f.service.rollback(PLAYER, CollectionId.CRYSTAL_COLLECTION, "crystal_01", true, 5L).code);
        PlayerState restored = f.unique.load(PLAYER);
        assertEquals(Rarity.COMMON, restored.collection(CollectionId.CRYSTAL_COLLECTION).owned.get("crystal_01"));
        assertEquals(Rarity.COMMON, restored.collection(CollectionId.CRYSTAL_COLLECTION).owned.get("crystal_02"));
        assertEquals(26, restored.collection(CollectionId.CRYSTAL_COLLECTION).progress());
        assertEquals(7, restored.collection(CollectionId.CRYSTAL_COLLECTION).enhancement("crystal_01"));
        assertEquals(5, restored.collection(CollectionId.CRYSTAL_COLLECTION).enhancement("crystal_02"));
        assertFalse(restored.collection(CollectionId.CRYSTAL_COLLECTION).canRollback("crystal_01"));
        assertEquals(UniqueRuneService.ResultCode.NO_ROLLBACK,
                f.service.rollback(PLAYER, CollectionId.CRYSTAL_COLLECTION, "crystal_01", true, 6L).code);
    }

    @Test void legacyCollectionRollbackIsNeverAppliedToAnIndividualRune() {
        Fixture f = new Fixture();
        PlayerState state = f.unique.load(PLAYER);
        state.collection(CollectionId.DAILY_COLLECTION).owned.put("daily_01", Rarity.IMPROVED);
        state.collection(CollectionId.DAILY_COLLECTION).rollbackRarity = Rarity.COMMON;
        state.collection(CollectionId.DAILY_COLLECTION).rollbackOwned.put("daily_01", Rarity.COMMON);
        state.collection(CollectionId.DAILY_COLLECTION).rollbackOwned.put("daily_02", Rarity.COMMON);
        f.unique.save(PLAYER, state);
        f.resources.setPlayerResourceValue(PLAYER, "crystals", new BigDecimal("1000"));
        BigDecimal before = f.resources.getPlayerResourceValue(PLAYER, "crystals");
        assertEquals(UniqueRuneService.ResultCode.NO_ROLLBACK,
                f.service.rollback(PLAYER, CollectionId.DAILY_COLLECTION, "daily_01", true, 1L).code);
        assertEquals(before, f.resources.getPlayerResourceValue(PLAYER, "crystals"));
        assertEquals(1, f.unique.load(PLAYER).collection(CollectionId.DAILY_COLLECTION).owned.size());
        assertFalse(f.unique.load(PLAYER).collection(CollectionId.DAILY_COLLECTION).owned.containsKey("daily_02"));
    }

    @Test void independentPlayersAndNonceIsolation() {
        Fixture f = new Fixture();
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000212");
        for (UUID player : new UUID[]{PLAYER, second}) {
            PlayerState state = f.unique.load(player);
            for (UniqueRuneModel.RuneDefinition rune : f.policy.definition(CollectionId.DAILY_COLLECTION).runes)
                state.collection(CollectionId.DAILY_COLLECTION).owned.put(rune.runeId, Rarity.COMMON);
            f.unique.save(player, state);
        }
        assertEquals(UniqueRuneService.ResultCode.OK,
                f.service.awaken(PLAYER, CollectionId.DAILY_COLLECTION, "daily_01", 1L).code);
        assertEquals(UniqueRuneService.ResultCode.REPLAYED,
                f.service.awaken(PLAYER, CollectionId.DAILY_COLLECTION, "daily_02", 1L).code);
        assertEquals(Rarity.IMPROVED, f.unique.load(PLAYER).collection(CollectionId.DAILY_COLLECTION).owned.get("daily_01"));
        assertEquals(Rarity.COMMON, f.unique.load(second).collection(CollectionId.DAILY_COLLECTION).owned.get("daily_01"));
        assertEquals(UniqueRuneService.ResultCode.OK,
                f.service.awaken(second, CollectionId.DAILY_COLLECTION, "daily_02", 1L).code);
    }

    @Test void onlyOwnedRuneCanBeSharedAndEnhancementIsCrystalOnly() {
        Fixture f = new Fixture();
        assertNull(f.service.share(PLAYER, CollectionId.CRYSTAL_COLLECTION, "crystal_01"));
        PlayerState state = f.unique.load(PLAYER);
        state.collection(CollectionId.CRYSTAL_COLLECTION).owned.put("crystal_01", Rarity.COMMON);
        f.unique.save(PLAYER, state);
        assertNotNull(f.service.share(PLAYER, CollectionId.CRYSTAL_COLLECTION, "crystal_01"));
        assertEquals(UniqueRuneService.ResultCode.INSUFFICIENT_RESOURCE, f.service.enhance(PLAYER, 1L).code);
    }

    @Test void randomEnhancementTargetsOnlyOwnedCrystalRunesAndChargesActualCost() {
        Fixture f = new Fixture(true);
        PlayerState state = f.unique.load(PLAYER);
        state.collection(CollectionId.CRYSTAL_COLLECTION).owned.put("crystal_01", Rarity.COMMON);
        state.collection(CollectionId.CRYSTAL_COLLECTION).owned.put("crystal_22", Rarity.COMMON);
        f.unique.save(PLAYER, state);
        f.resources.setPlayerResourceValue(PLAYER, "pearl", new BigDecimal("1000"));
        BigDecimal cost = f.policy.enhancementPearlCost("crystal_22", 0);

        UniqueRuneService.Result result = f.service.enhance(PLAYER, 1L);
        assertEquals(UniqueRuneService.ResultCode.OK, result.code);
        assertEquals("crystal_22", result.runeId);
        PlayerState updated = f.unique.load(PLAYER);
        assertEquals(0, updated.collection(CollectionId.CRYSTAL_COLLECTION).enhancement("crystal_01"));
        assertEquals(1, updated.collection(CollectionId.CRYSTAL_COLLECTION).enhancement("crystal_22"));
        assertEquals(0, new BigDecimal("1000").subtract(cost)
                .compareTo(f.resources.getPlayerResourceValue(PLAYER, "pearl")));
        assertEquals(UniqueRuneService.ResultCode.REPLAYED, f.service.enhance(PLAYER, 1L).code);
    }

    @Test void randomEnhancementRequiresOwnedRuneAndAffordablePearls() {
        Fixture f = new Fixture();
        assertEquals(UniqueRuneService.ResultCode.INVALID, f.service.enhance(PLAYER, 1L).code);
        PlayerState state = f.unique.load(PLAYER);
        state.collection(CollectionId.CRYSTAL_COLLECTION).owned.put("crystal_03", Rarity.COMMON);
        f.unique.save(PLAYER, state);
        assertEquals(UniqueRuneService.ResultCode.INSUFFICIENT_RESOURCE, f.service.enhance(PLAYER, 2L).code);
        assertEquals(0, f.unique.load(PLAYER).collection(CollectionId.CRYSTAL_COLLECTION).enhancement("crystal_03"));
        assertEquals(BigDecimal.ZERO, f.resources.getPlayerResourceValue(PLAYER, "pearl"));
    }

    private static final class Fixture {
        final MemUniqueRepository unique = new MemUniqueRepository();
        final MemResourceRepository resourceRepo = new MemResourceRepository();
        final ResourceService resources = new ResourceService(resourceRepo);
        final BalanceUniqueRunePolicy policy = new BalanceUniqueRunePolicy(BalanceBootstrap.activateBundledDefaults());
        BigDecimal offerings = BigDecimal.ZERO;
        final UniqueRuneService service;

        Fixture() { this(false); }
        Fixture(boolean chooseLast) {
            service = new UniqueRuneService(unique, policy, resources, uuid -> Long.MAX_VALUE, uuid -> true,
                    new UniqueRuneService.OfferingsWallet() {
                        @Override public BigDecimal balance(UUID uuid) { return offerings; }
                        @Override public boolean spend(UUID uuid, BigDecimal amount) {
                            if (offerings.compareTo(amount) < 0) return false;
                            offerings = offerings.subtract(amount); return true;
                        }
                    }, bound -> chooseLast ? bound - 1 : 0);
        }
    }

    private static final class MemUniqueRepository implements UniqueRuneRepository {
        private final Map<UUID, PlayerState> states = new HashMap<>();
        @Override public PlayerState load(UUID uuid) { return states.computeIfAbsent(uuid, key -> new PlayerState()).copy(); }
        @Override public void save(UUID uuid, PlayerState state) { states.put(uuid, state.copy()); }
    }

    private static final class MemResourceRepository implements ResourceRepository {
        private final Map<String, BigDecimal> values = new HashMap<>();
        private String key(UUID uuid, String resource) { return uuid + ":" + resource; }
        @Override public void purgeDeprecatedLightnings() {}
        @Override public void purgeDeprecatedLightnings(UUID uuid) {}
        @Override public BigDecimal getPlayerResourceValue(UUID uuid, String resource) { return values.getOrDefault(key(uuid, resource), BigDecimal.ZERO); }
        @Override public void writePlayerResource(UUID uuid, String resource, BigDecimal value) { values.put(key(uuid, resource), value); }
    }
}
