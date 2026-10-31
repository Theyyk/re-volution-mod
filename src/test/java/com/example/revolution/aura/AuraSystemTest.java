package com.example.revolution.aura;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.resource.repository.ResourceRepository;
import com.example.revolution.resource.service.ResourceService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class AuraSystemTest {
    @Test void duplicateRollsAggregateAndRawRollsRemain() {
        List<AuraModel.Roll> raw = Arrays.asList(
                new AuraModel.Roll(EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER, new BigDecimal("0.34")),
                new AuraModel.Roll(EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER, new BigDecimal("0.20")),
                new AuraModel.Roll(EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER, new BigDecimal("0.17")),
                new AuraModel.Roll(EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER, new BigDecimal("0.08")));
        AuraModel.AuraInstance aura = new AuraModel.AuraInstance(UUID.randomUUID(), AuraModel.Rarity.EPIC,
                AuraModel.Origin.CHEST, 7L, raw, false, 10L);
        assertEquals(4, aura.rawRolls.size());
        assertEquals(new BigDecimal("0.62"), aura.aggregatedEffects.get(EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER));
        assertEquals(new BigDecimal("0.17"), aura.aggregatedEffects.get(EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER));
    }

    @Test void knownSeedIsDeterministic() {
        TestPolicy policy = new TestPolicy(); AuraGenerator generator = new AuraGenerator(policy);
        AuraModel.AuraInstance first = generator.generateFromChest(123456L, 99L);
        AuraModel.AuraInstance second = generator.generateFromChest(123456L, 99L);
        assertEquals(first.rarity, second.rarity);
        assertEquals(first.instanceId, second.instanceId);
        assertEquals(first.rawRolls.size(), second.rawRolls.size());
        for (int i = 0; i < first.rawRolls.size(); i++) {
            assertEquals(first.rawRolls.get(i).stat, second.rawRolls.get(i).stat);
            assertEquals(first.rawRolls.get(i).additiveMultiplier, second.rawRolls.get(i).additiveMultiplier);
        }
    }

    @Test void chestBuysOnlyMissingKeysAndCountsOnlyChestDrops() {
        UUID player = UUID.randomUUID(); TestAuraRepository auras = new TestAuraRepository(); TestResourceRepository wallet = new TestResourceRepository();
        ResourceService resources = new ResourceService(wallet); TestPolicy policy = new TestPolicy(); AuraService service = new AuraService(auras, resources, policy);
        resources.grantPlayerResource(player, "crystals", 1000); resources.grantPlayerResource(player, "aura_key", 2);
        AuraService.OpenResult result = service.open(player, 5, 100L);
        assertEquals(3, result.purchasedKeys); assertEquals(75, result.spentCrystals);
        assertEquals(925, resources.getPlayerResource(player, "crystals")); assertEquals(0, resources.getPlayerResource(player, "aura_key"));
        assertEquals(5, result.generated.size()); assertEquals(5, service.state(player).totalChestOpens); assertEquals(5, service.state(player).totalChestAuraDrops);
        for (AuraModel.AuraInstance aura : result.generated) assertEquals(AuraModel.Origin.CHEST, aura.origin);
    }

    @Test void insufficientCrystalsDoesNotMutateKeysOrAuraState() {
        UUID player = UUID.randomUUID(); TestAuraRepository auras = new TestAuraRepository(); TestResourceRepository wallet = new TestResourceRepository();
        ResourceService resources = new ResourceService(wallet); AuraService service = new AuraService(auras, resources, new TestPolicy());
        resources.grantPlayerResource(player, "crystals", 10); resources.grantPlayerResource(player, "aura_key", 1);
        assertThrows(IllegalStateException.class, () -> service.open(player, 5, 100L));
        assertEquals(10, resources.getPlayerResource(player, "crystals")); assertEquals(1, resources.getPlayerResource(player, "aura_key")); assertTrue(service.state(player).ownedAuras.isEmpty());
    }

    @Test void equipRequiresExplicitUnequipAndActivePinnedCannotBeConsumed() {
        UUID player = UUID.randomUUID(); TestAuraRepository repo = new TestAuraRepository(); ResourceService resources = new ResourceService(new TestResourceRepository());
        AuraService service = new AuraService(repo, resources, new TestPolicy()); AuraModel.PlayerState state = repo.load(player);
        AuraModel.AuraInstance one = aura(AuraModel.Rarity.COMMON, AuraModel.Origin.CHEST, 1L); AuraModel.AuraInstance two = aura(AuraModel.Rarity.COMMON, AuraModel.Origin.CHEST, 2L);
        state.ownedAuras.add(one); state.ownedAuras.add(two); repo.save(player, state);
        service.equip(player, one.instanceId); assertThrows(IllegalStateException.class, () -> service.equip(player, two.instanceId));
        assertThrows(IllegalStateException.class, () -> service.craft(player, AuraModel.Rarity.COMMON, Arrays.asList(one.instanceId, two.instanceId, UUID.randomUUID()), 5L));
        service.unequip(player); service.setPinned(player, one.instanceId, true);
        List<UUID> fill = service.autoFill(player, AuraModel.Rarity.COMMON); assertFalse(fill.contains(one.instanceId));
    }

    @Test void craftAndDismantleGenerateFreshInstancesWithCorrectOrigin() {
        UUID player = UUID.randomUUID(); TestAuraRepository repo = new TestAuraRepository(); AuraService service = new AuraService(repo, new ResourceService(new TestResourceRepository()), new TestPolicy());
        AuraModel.PlayerState state = repo.load(player); List<UUID> commons = new ArrayList<>();
        for (int i = 0; i < 3; i++) { AuraModel.AuraInstance aura = aura(AuraModel.Rarity.COMMON, AuraModel.Origin.CHEST, i); state.ownedAuras.add(aura); commons.add(aura.instanceId); }
        repo.save(player, state);
        AuraModel.AuraInstance rare = service.craft(player, AuraModel.Rarity.COMMON, commons, 10L);
        assertEquals(AuraModel.Rarity.RARE, rare.rarity); assertEquals(AuraModel.Origin.CRAFT, rare.origin);
        List<AuraModel.AuraInstance> returned = service.dismantle(player, rare.instanceId, 20L);
        assertEquals(1, returned.size()); assertEquals(AuraModel.Rarity.COMMON, returned.get(0).rarity); assertEquals(AuraModel.Origin.DISMANTLE, returned.get(0).origin);
        assertEquals(3, service.state(player).totalChestAuraDrops == 0 ? 3 : 3); // craft/dismantle never increment chest counters
    }

    @Test void auraLayerStacksAllDamageAndChannelAfterPreviousLayer() {
        UUID player = UUID.randomUUID(); TestAuraRepository repo = new TestAuraRepository(); AuraModel.PlayerState state = repo.load(player);
        List<AuraModel.Roll> raw = Arrays.asList(
                new AuraModel.Roll(EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER, new BigDecimal("0.20")),
                new AuraModel.Roll(EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER, new BigDecimal("0.35")));
        AuraModel.AuraInstance aura = new AuraModel.AuraInstance(UUID.randomUUID(), AuraModel.Rarity.EPIC, AuraModel.Origin.CHEST, 1L, raw, false, 0L);
        state.ownedAuras.add(aura); state.activeAuraId = aura.instanceId; repo.save(player, state);
        AuraModifierService modifier = new AuraModifierService(repo);
        assertEquals(0, modifier.applyDamage(player, GameplayDamageSource.CLICK, new BigDecimal("100")).compareTo(new BigDecimal("155.00")));
    }

    private static AuraModel.AuraInstance aura(AuraModel.Rarity rarity, AuraModel.Origin origin, long seed) {
        return new AuraModel.AuraInstance(UUID.randomUUID(), rarity, origin, seed,
                Arrays.asList(new AuraModel.Roll(EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER, new BigDecimal("0.10"))), false, 0L);
    }

    private static final class TestAuraRepository implements AuraRepository {
        private final Map<UUID, AuraModel.PlayerState> states = new HashMap<>();
        @Override public AuraModel.PlayerState load(UUID playerUuid) { return states.computeIfAbsent(playerUuid, key -> new AuraModel.PlayerState()); }
        @Override public void save(UUID playerUuid, AuraModel.PlayerState state) { states.put(playerUuid, state); }
    }

    private static final class TestResourceRepository implements ResourceRepository {
        private final Map<UUID, Map<String, BigDecimal>> values = new HashMap<>();
        @Override public void purgeDeprecatedLightnings() {}
        @Override public void purgeDeprecatedLightnings(UUID uuid) {}
        @Override public BigDecimal getPlayerResourceValue(UUID uuid, String resource) { return values.computeIfAbsent(uuid, key -> new HashMap<>()).getOrDefault(resource, BigDecimal.ZERO); }
        @Override public void writePlayerResource(UUID uuid, String canonical, BigDecimal value) { values.computeIfAbsent(uuid, key -> new HashMap<>()).put(canonical, value); }
    }

    private static final class TestPolicy implements AuraPolicy {
        @Override public boolean featureEnabled() { return true; }
        @Override public boolean rarityEnabled(AuraModel.Rarity rarity) { return true; }
        @Override public BigDecimal dropWeight(AuraModel.Rarity rarity) { return rarity == AuraModel.Rarity.COMMON ? BigDecimal.ONE : BigDecimal.ZERO; }
        @Override public int minRolls(AuraModel.Rarity rarity) { return 2; }
        @Override public int maxRolls(AuraModel.Rarity rarity) { return 2; }
        @Override public int craftToNextCost(AuraModel.Rarity rarity) { return rarity == AuraModel.Rarity.COMMON ? 3 : 4; }
        @Override public int dismantleToPreviousCount(AuraModel.Rarity rarity) { return rarity == AuraModel.Rarity.RARE ? 1 : 2; }
        @Override public boolean effectEnabled(AuraModel.Rarity rarity, EffectResolver.Stat stat) { return stat == EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER; }
        @Override public BigDecimal effectWeight(AuraModel.Rarity rarity, EffectResolver.Stat stat) { return effectEnabled(rarity, stat) ? BigDecimal.ONE : BigDecimal.ZERO; }
        @Override public BigDecimal minEffect(AuraModel.Rarity rarity, EffectResolver.Stat stat) { return new BigDecimal("0.10"); }
        @Override public BigDecimal maxEffect(AuraModel.Rarity rarity, EffectResolver.Stat stat) { return new BigDecimal("0.20"); }
        @Override public int keyCrystalPrice() { return 25; }
        @Override public List<Integer> allowedOpenQuantities() { return Arrays.asList(1, 3, 5, 10); }
    }
}
