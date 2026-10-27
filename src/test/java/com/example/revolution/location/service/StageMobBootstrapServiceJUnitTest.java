package com.example.revolution.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationKind;
import com.example.revolution.location.definition.LocationStageTemplate;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.MovementMode;
import com.example.revolution.location.definition.SpawnMode;
import com.example.revolution.location.definition.SpawnRealmScope;
import com.example.revolution.location.definition.StoredMobSpawn;
import com.example.revolution.location.generation.LocationGenerationPlanner;
import com.example.revolution.location.generation.LocationTemplate;
import com.example.revolution.location.repository.InMemoryLocationRepository;
import com.example.revolution.location.repository.LocationStageTemplateRepository;
import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.repository.MobRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public final class StageMobBootstrapServiceJUnitTest {
    @Test
    void locationCreateAutomaticallyCreatesCanonicalMobAndPopulation() {
        InMemoryLocationRepository locations = new InMemoryLocationRepository();
        MemoryMobs mobs = new MemoryMobs();
        PhysicalBuildTestSupport storage = PhysicalBuildTestSupport.configured();
        StageMobBootstrapService stageMobs = new StageMobBootstrapService(mobs, locations, storage);
        BootstrappingLocationGenerationRepository generations =
                new BootstrappingLocationGenerationRepository(storage, stageMobs);
        LocationProvisioningService provisioning = new LocationProvisioningService(
                locations,
                new LocationGenerationPlanner(),
                LocationTemplate.defaultStage(),
                new MemoryTemplates(),
                null,
                storage,
                storage,
                generations);
        provisioning.setStageTemplate(2, 30.0D, 5.0D, 5.0D);

        provisioning.createStage(2, 3, 12.0D, 72.0D, 24.0D, 99L, 123L, storage);

        MobDefinition mob = mobs.getMob("Mob2_3");
        assertNotNull(mob);
        assertEquals("zombie", mob.type);
        assertEquals(0, BigDecimal.ONE.compareTo(mob.hp));
        assertEquals("coins", mobs.getMobResource("Mob2_3"));
        assertEquals(0, BigDecimal.ONE.compareTo(mobs.getMobAmountValue("Mob2_3")));

        List<StoredMobSpawn> spawns = locations.getMobSpawns(MobZoneIds.encode(2, 3));
        assertEquals(1, spawns.size());
        StoredMobSpawn spawn = spawns.get(0);
        assertEquals("Mob2_3", spawn.getMobName());
        assertEquals(12.0D, spawn.getSpawnX(), 0.0D);
        assertEquals(72.0D, spawn.getSpawnY(), 0.0D);
        assertEquals(24.0D, spawn.getSpawnZ(), 0.0D);
        assertEquals(SpawnMode.FIXED_POINT, spawn.getSpawnMode());
        assertEquals(MovementMode.ROAMING, spawn.getMovementMode());
        assertEquals(SpawnRealmScope.PLAYER_REALMS, spawn.getRealmScope());
    }

    @Test
    void existingDefinitionIsNeverOverwrittenByNewStageBootstrap() {
        InMemoryLocationRepository locations = new InMemoryLocationRepository();
        MemoryMobs mobs = new MemoryMobs();
        PhysicalBuildTestSupport generations = PhysicalBuildTestSupport.configured();
        int internalId = MobZoneIds.encode(4, 7);
        locations.seed(new LocationDefinition(internalId, "MobZone 4:7", 10.0D, 20.0D, 25.0D, LocationKind.MOB_ZONE));
        mobs.createMob("Mob4_7", "skeleton", new BigDecimal("999"), 25);
        mobs.setMobReward("Mob4_7", "rune_dust", new BigDecimal("25"));

        new StageMobBootstrapService(mobs, locations, generations).bootstrapNewStage(4, 7, 66.0D);

        MobDefinition mob = mobs.getMob("Mob4_7");
        assertEquals("skeleton", mob.type);
        assertEquals(0, new BigDecimal("999").compareTo(mob.hp));
        assertEquals("rune_dust", mobs.getMobResource("Mob4_7"));
        assertEquals(0, new BigDecimal("25").compareTo(mobs.getMobAmountValue("Mob4_7")));
        assertEquals(1, locations.getMobSpawns(internalId).size());
    }

    @Test
    void reconciliationReplacesEmptyOrTestPopulationOnceAndPreservesLaterCanonicalCount() {
        InMemoryLocationRepository locations = new InMemoryLocationRepository();
        MemoryMobs mobs = new MemoryMobs();
        PhysicalBuildTestSupport generations = PhysicalBuildTestSupport.configured();
        int stage1 = MobZoneIds.encode(1, 1);
        int stage2 = MobZoneIds.encode(1, 2);
        locations.seed(
                new LocationDefinition(stage1, "MobZone 1:1", 0.0D, 0.0D, 20.0D, LocationKind.MOB_ZONE),
                new LocationDefinition(stage2, "MobZone 1:2", 0.0D, 68.0D, 20.0D, LocationKind.MOB_ZONE));
        locations.upsertMobSpawn(spawn(stage1, 1, "TestMob", 64.0D));

        StageMobBootstrapService stageMobs = new StageMobBootstrapService(mobs, locations, generations);
        assertEquals(2, stageMobs.reconcileConfiguredRange(1, 1, 2));
        assertEquals("Mob1_1", locations.getMobSpawns(stage1).get(0).getMobName());
        assertEquals("Mob1_2", locations.getMobSpawns(stage2).get(0).getMobName());
        assertEquals(1, locations.getMobSpawns(stage1).size());
        assertEquals(1, locations.getMobSpawns(stage2).size());

        locations.upsertMobSpawn(spawn(stage1, 2, "Mob1_1", 64.0D));
        assertEquals(0, stageMobs.reconcileConfiguredRange(1, 1, 2));
        assertEquals(2, locations.getMobSpawns(stage1).size());
    }

    private static StoredMobSpawn spawn(int locationId, int spawnId, String mobName, double y) {
        return new StoredMobSpawn(locationId, spawnId, mobName, 0.0D, y, 0.0D, 0.0F, 0.0F,
                SpawnMode.FIXED_POINT, MovementMode.ROAMING, SpawnRealmScope.PLAYER_REALMS, true);
    }

    private static final class MemoryTemplates implements LocationStageTemplateRepository {
        private final Map<Integer, LocationStageTemplate> values = new HashMap<>();
        @Override public LocationStageTemplate get(int worldLocationId) { return values.get(worldLocationId); }
        @Override public void upsert(LocationStageTemplate template) { values.put(template.getWorldLocationId(), template); }
        @Override public void delete(int worldLocationId) { values.remove(worldLocationId); }
    }

    private static final class MemoryMobs implements MobRepository {
        private final Map<String, MobDefinition> definitions = new HashMap<>();
        private final Map<String, String> resources = new HashMap<>();
        private final Map<String, BigDecimal> amounts = new HashMap<>();

        @Override public void createMob(String name, String type, BigDecimal hp, int money) {
            definitions.put(name, new MobDefinition(name, type, hp, money, 0.0D, 0.0D, 0.0D, false));
        }
        @Override public void setMobSpawn(String name, double x, double y, double z) { }
        @Override public void unspawnMob(String name) { }
        @Override public void deleteMob(String name) {
            definitions.remove(name);
            resources.remove(name);
            amounts.remove(name);
        }
        @Override public List<MobDefinition> getAllMobs() { return new ArrayList<>(definitions.values()); }
        @Override public MobDefinition getMob(String name) { return definitions.get(name); }
        @Override public void setMobReward(String mobName, String resource, BigDecimal amount) {
            resources.put(mobName, resource);
            amounts.put(mobName, amount);
        }
        @Override public void setMobReward(String mobName, String resource, int amount) {
            setMobReward(mobName, resource, BigDecimal.valueOf(amount));
        }
        @Override public String getMobResource(String mobName) { return resources.get(mobName); }
        @Override public BigDecimal getMobAmountValue(String mobName) { return amounts.getOrDefault(mobName, BigDecimal.ZERO); }
        @Override public int getMobAmount(String mobName) { return getMobAmountValue(mobName).intValue(); }
    }
}
