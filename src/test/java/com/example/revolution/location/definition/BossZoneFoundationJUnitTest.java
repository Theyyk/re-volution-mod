package com.example.revolution.location.definition;

import com.example.revolution.mob.repository.InMemoryMobProgressionRepository;
import com.example.revolution.mob.service.MobProgressionService;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BossZoneFoundationJUnitTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000001311");

    @Test void legacyConstructorsRemainNormalMobZones() {
        int internalId = MobZoneIds.encode(1, 1);
        LocationDefinition legacy = new LocationDefinition(internalId, "legacy", 0.0D, 0.0D, 5.0D);
        assertEquals(LocationKind.MOB_ZONE, legacy.getKind());
        assertTrue(legacy.isMobStageZone());
        assertFalse(legacy.isBossZone());
    }

    @Test void bossZoneRemainsAvailableAsPersistedLegacyIdentity() {
        int internalId = MobZoneIds.encode(1, 9);
        LocationDefinition boss = new LocationDefinition(internalId, "boss", 0.0D, 0.0D, 5.0D, LocationKind.BOSS_ZONE);
        assertEquals(LocationKind.BOSS_ZONE, boss.getKind());
        assertTrue(boss.isMobStageZone());
        assertTrue(boss.isBossZone());
    }

    @Test void kindMustMatchLocationIdNamespace() {
        assertThrows(IllegalArgumentException.class,
                () -> new LocationDefinition(1, "world", 0.0D, 0.0D, 5.0D, LocationKind.BOSS_ZONE));
        assertThrows(IllegalArgumentException.class,
                () -> new LocationDefinition(MobZoneIds.encode(1, 1), "zone", 0.0D, 0.0D, 5.0D, LocationKind.WORLD));
    }

    @Test void finalConfiguredStageIsRuntimeBossWithoutExplicitBossKind() {
        MobProgressionService progression = new MobProgressionService(new InMemoryMobProgressionRepository());
        LocationDefinition first = zone(1, 1, LocationKind.MOB_ZONE);
        LocationDefinition last = zone(1, 2, LocationKind.MOB_ZONE);
        LocationDefinition next = zone(2, 1, LocationKind.MOB_ZONE);
        progression.reloadConfiguration(
                Arrays.asList(first, last, next),
                Arrays.asList(spawn(1, 1), spawn(1, 2), spawn(2, 1)));

        assertTrue(progression.hasValidBossLayout(1));
        assertFalse(progression.isBossStage(MobZoneIds.encode(1, 1)));
        assertTrue(progression.isBossStage(MobZoneIds.encode(1, 2)));
        assertTrue(progression.isFinalStage(MobZoneIds.encode(1, 2)));
        assertEquals(Integer.valueOf(MobZoneIds.encode(2, 1)),
                progression.nextConfiguredInternalLocationId(MobZoneIds.encode(1, 2)));
    }

    @Test void persistedEarlierBossCannotOverrideFinalConfiguredStage() {
        MobProgressionService progression = new MobProgressionService(new InMemoryMobProgressionRepository());
        progression.reloadConfiguration(
                Arrays.asList(
                        zone(1, 1, LocationKind.MOB_ZONE),
                        zone(1, 2, LocationKind.BOSS_ZONE),
                        zone(1, 3, LocationKind.MOB_ZONE)),
                Arrays.asList(spawn(1, 1), spawn(1, 2), spawn(1, 3)));

        assertTrue(progression.hasValidBossLayout(1));
        assertFalse(progression.isBossStage(MobZoneIds.encode(1, 2)));
        assertTrue(progression.isBossStage(MobZoneIds.encode(1, 3)));
        assertFalse(progression.isFinalStage(MobZoneIds.encode(1, 2)));
        assertTrue(progression.isFinalStage(MobZoneIds.encode(1, 3)));
    }

    @Test void addingLaterConfiguredStageMovesRuntimeBossOnReload() {
        MobProgressionService progression = new MobProgressionService(new InMemoryMobProgressionRepository());
        progression.reloadConfiguration(
                Arrays.asList(zone(1, 1, LocationKind.MOB_ZONE), zone(1, 2, LocationKind.MOB_ZONE)),
                Arrays.asList(spawn(1, 1), spawn(1, 2)));
        assertTrue(progression.isBossStage(MobZoneIds.encode(1, 2)));

        progression.reloadConfiguration(
                Arrays.asList(
                        zone(1, 1, LocationKind.MOB_ZONE),
                        zone(1, 2, LocationKind.MOB_ZONE),
                        zone(1, 3, LocationKind.MOB_ZONE)),
                Arrays.asList(spawn(1, 1), spawn(1, 2), spawn(1, 3)));

        assertFalse(progression.isBossStage(MobZoneIds.encode(1, 2)));
        assertTrue(progression.isBossStage(MobZoneIds.encode(1, 3)));
    }

    private static LocationDefinition zone(int location, int slot, LocationKind kind) {
        return new LocationDefinition(MobZoneIds.encode(location, slot), "zone", slot * 10.0D, location * 100.0D, 5.0D, kind);
    }

    private static StoredMobSpawn spawn(int location, int slot) {
        return new StoredMobSpawn(MobZoneIds.encode(location, slot), slot, "mob-" + location + "-" + slot,
                slot * 10.0D, 64.0D, location * 100.0D, 0.0F, 0.0F,
                SpawnMode.FIXED_POINT, MovementMode.STATIONARY, SpawnRealmScope.PLAYER_REALMS, true);
    }
}
