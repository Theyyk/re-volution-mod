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

    @Test void awakeningRequiresFullCollectionAndRollbackRestoresExactPriorState() {
        Fixture f = new Fixture();
        assertEquals(UniqueRuneService.ResultCode.NOT_READY,
                f.service.awaken(PLAYER, CollectionId.CRYSTAL_COLLECTION, "crystal_01", 1L).code);

        PlayerState state = f.unique.load(PLAYER);
        for (UniqueRuneModel.RuneDefinition rune : f.policy.definition(CollectionId.CRYSTAL_COLLECTION).runes)
            state.collection(CollectionId.CRYSTAL_COLLECTION).owned.put(rune.runeId, Rarity.COMMON);
        state.collection(CollectionId.CRYSTAL_COLLECTION).enhancement.put("crystal_01", 7);
        f.unique.save(PLAYER, state);

        assertEquals(UniqueRuneService.ResultCode.OK,
                f.service.awaken(PLAYER, CollectionId.CRYSTAL_COLLECTION, "crystal_01", 2L).code);
        PlayerState awakened = f.unique.load(PLAYER);
        assertEquals(Rarity.IMPROVED, awakened.collection(CollectionId.CRYSTAL_COLLECTION).currentRarity);
        assertEquals(1, awakened.collection(CollectionId.CRYSTAL_COLLECTION).progress());
        assertEquals(7, awakened.collection(CollectionId.CRYSTAL_COLLECTION).enhancement("crystal_01"));

        assertEquals(UniqueRuneService.ResultCode.CONFIRMATION_REQUIRED,
                f.service.rollback(PLAYER, CollectionId.CRYSTAL_COLLECTION, false, 3L).code);
        f.resources.setPlayerResourceValue(PLAYER, "crystals", new BigDecimal("1000"));
        assertEquals(UniqueRuneService.ResultCode.OK,
                f.service.rollback(PLAYER, CollectionId.CRYSTAL_COLLECTION, true, 4L).code);
        PlayerState restored = f.unique.load(PLAYER);
        assertEquals(Rarity.COMMON, restored.collection(CollectionId.CRYSTAL_COLLECTION).currentRarity);
        assertEquals(26, restored.collection(CollectionId.CRYSTAL_COLLECTION).progress());
        assertEquals(7, restored.collection(CollectionId.CRYSTAL_COLLECTION).enhancement("crystal_01"));
        assertNull(restored.collection(CollectionId.CRYSTAL_COLLECTION).rollbackRarity);
    }

    @Test void onlyOwnedRuneCanBeSharedAndEnhancementIsCrystalOnly() {
        Fixture f = new Fixture();
        assertNull(f.service.share(PLAYER, CollectionId.CRYSTAL_COLLECTION, "crystal_01"));
        PlayerState state = f.unique.load(PLAYER);
        state.collection(CollectionId.CRYSTAL_COLLECTION).owned.put("crystal_01", Rarity.COMMON);
        f.unique.save(PLAYER, state);
        assertNotNull(f.service.share(PLAYER, CollectionId.CRYSTAL_COLLECTION, "crystal_01"));
        assertEquals(UniqueRuneService.ResultCode.INVALID, f.service.enhance(PLAYER, "daily_01", 1L).code);
    }

    private static final class Fixture {
        final MemUniqueRepository unique = new MemUniqueRepository();
        final MemResourceRepository resourceRepo = new MemResourceRepository();
        final ResourceService resources = new ResourceService(resourceRepo);
        final BalanceUniqueRunePolicy policy = new BalanceUniqueRunePolicy(BalanceBootstrap.activateBundledDefaults());
        BigDecimal offerings = BigDecimal.ZERO;
        final UniqueRuneService service;

        Fixture() {
            service = new UniqueRuneService(unique, policy, resources, uuid -> Long.MAX_VALUE, uuid -> true,
                    new UniqueRuneService.OfferingsWallet() {
                        @Override public BigDecimal balance(UUID uuid) { return offerings; }
                        @Override public boolean spend(UUID uuid, BigDecimal amount) {
                            if (offerings.compareTo(amount) < 0) return false;
                            offerings = offerings.subtract(amount); return true;
                        }
                    }, bound -> 0);
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
