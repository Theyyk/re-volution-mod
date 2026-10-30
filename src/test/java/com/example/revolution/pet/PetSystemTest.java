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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class PetSystemTest {
    @Test public void purchaseIsOncePerRunAndAdvancedOwnershipPersists() {
        Fixture f = new Fixture();
        f.resources.put("coins", new BigDecimal("100000"));
        assertEquals(PetService.Result.APPLIED, f.pets.purchasePet(f.id, "pet_01"));
        BigDecimal after = f.resources.get("coins");
        assertEquals(PetService.Result.ALREADY_OWNED, f.pets.purchasePet(f.id, "pet_01"));
        assertEquals(after, f.resources.get("coins"));

        PetPlayerState state = f.petRepo.load(f.id);
        state.pet("pet_01").rank = PetRank.ADVANCED;
        f.petRepo.save(f.id, state);
        f.startNewRun();
        assertEquals(PetService.Result.ALREADY_OWNED, f.pets.purchasePet(f.id, "pet_01"));
    }

    @Test public void awakeningRequiresFullBoardAndCooldownSkipsPremiumSlot() {
        Fixture f = new Fixture();
        f.resources.put("coins", new BigDecimal("100000"));
        f.resources.put("crystals", new BigDecimal("100000"));
        f.fragments(new BigDecimal("100000"));
        assertEquals(PetService.Result.APPLIED, f.pets.purchaseNormalSlot(f.id));
        assertEquals(PetService.Result.APPLIED, f.pets.purchasePremiumSlot(f.id));
        f.pets.purchasePet(f.id, "pet_01"); f.pets.purchasePet(f.id, "pet_02");
        f.pets.selectPet(f.id, "pet_01");
        assertEquals(PetService.Result.BOARD_NOT_FULL, f.pets.awaken(f.id, "pet_01"));
        f.pets.selectPet(f.id, "pet_02");
        assertEquals(PetService.Result.APPLIED, f.pets.awaken(f.id, "pet_01"));
        PetPlayerState state = f.pets.snapshot(f.id);
        assertEquals(PetRank.IMPROVED, state.pets.get("pet_01").rank);
        assertFalse(state.run.cooldownUntil.containsKey("pet_02"));
    }

    @Test public void xpIsUncappedAndPetXpMultiplierSelfBenefits() {
        Fixture f = new Fixture();
        f.resources.put("coins", new BigDecimal("100000"));
        f.fragments(new BigDecimal("100000"));
        f.pets.purchaseNormalSlot(f.id); f.pets.purchasePet(f.id, "pet_09"); f.pets.selectPet(f.id, "pet_09");
        PetEffectSource source = new PetEffectSource(f.petRepo, f.catalog, f.petBalance);
        EffectResolver resolver = new EffectResolver(); resolver.register(source);
        BigDecimal multiplier = resolver.multiplier(f.id, EffectResolver.Stat.PET_XP_MULTIPLIER);
        assertTrue(multiplier.compareTo(BigDecimal.ONE) > 0);
        f.pets.accrueOnlineXp(f.id, 200000L, multiplier);
        assertTrue(f.pets.snapshot(f.id).pets.get("pet_09").level.compareTo(java.math.BigInteger.ONE) > 0);
    }

    @Test public void tradeoffCanResolveExactZeroAndRevertIsFree() {
        Fixture f = new Fixture();
        f.resources.put("coins", new BigDecimal("100000")); f.fragments(new BigDecimal("100000"));
        f.pets.purchaseNormalSlot(f.id); f.pets.purchasePet(f.id, "pet_13"); f.pets.selectPet(f.id, "pet_13");
        EffectResolver resolver = new EffectResolver(); resolver.register(new PetEffectSource(f.petRepo, f.catalog, f.petBalance));
        assertEquals(0, resolver.multiplier(f.id, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER).compareTo(BigDecimal.ZERO));
        PetPlayerState state = f.petRepo.load(f.id); state.pet("pet_13").rank = PetRank.MYTHIC; f.petRepo.save(f.id, state);
        assertEquals(PetService.Result.APPLIED, f.pets.revertRank(f.id, "pet_13"));
        assertEquals(PetRank.ADVANCED, f.pets.snapshot(f.id).pets.get("pet_13").rank);
    }

    private static final class Fixture {
        final UUID id = UUID.randomUUID();
        final MemoryPetRepo petRepo = new MemoryPetRepo();
        final MemoryResourceRepo resources = new MemoryResourceRepo(id);
        final MemoryPrestigeRepo prestigeRepo = new MemoryPrestigeRepo();
        final PrestigeService prestige = new PrestigeService(prestigeRepo, new PrestigeBranchRegistry());
        final PetCatalog catalog = new PetCatalog();
        final PetBalanceService petBalance = new PetBalanceService(BalanceBootstrap.activateBundledDefaults());
        final PetService pets = new PetService(petRepo, catalog, petBalance, new ResourceService(resources), prestige);
        Fixture() { prestige.startRun(id, PrestigeBranch.CLICK); }
        void fragments(BigDecimal value) { PrestigeState s = prestigeRepo.load(id); s.activeRun.runBranchCurrency = value; prestigeRepo.save(id, s); }
        void startNewRun() { PrestigeState s = prestigeRepo.load(id); s.activeRun = null; prestigeRepo.save(id, s); prestige.startRun(id, PrestigeBranch.CLICK); }
    }

    private static final class MemoryPetRepo implements PetRepository {
        private final Map<UUID, PetPlayerState> values = new HashMap<>();
        public PetPlayerState load(UUID id) { PetPlayerState s = values.get(id); return s == null ? new PetPlayerState() : s.copy(); }
        public void save(UUID id, PetPlayerState state) { values.put(id, state.copy()); }
    }
    private static final class MemoryResourceRepo implements ResourceRepository {
        private final UUID id; private final Map<String, BigDecimal> values = new HashMap<>();
        MemoryResourceRepo(UUID id) { this.id = id; }
        void put(String key, BigDecimal value) { values.put(key, value); }
        BigDecimal get(String key) { return getPlayerResourceValue(id, key); }
        public void purgeDeprecatedLightnings() {} public void purgeDeprecatedLightnings(UUID uuid) {}
        public BigDecimal getPlayerResourceValue(UUID uuid, String resource) { BigDecimal v = values.get(resource); return v == null ? BigDecimal.ZERO : v; }
        public void writePlayerResource(UUID uuid, String canonical, BigDecimal value) { values.put(canonical, value); }
    }
    private static final class MemoryPrestigeRepo implements PrestigeRepository {
        private final Map<UUID, PrestigeState> values = new HashMap<>();
        public PrestigeState load(UUID id) { PrestigeState s = values.get(id); return s == null ? new PrestigeState() : s.copy(); }
        public void save(UUID id, PrestigeState state) { values.put(id, state.copy()); }
        public ResetCommitResult commitReset(UUID id, String run, PrestigeState state) { values.put(id, state.copy()); return ResetCommitResult.APPLIED; }
    }
}
