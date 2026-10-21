package com.example.revolution.location;

import com.example.revolution.location.definition.LocationCatalog;
import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobSpawnDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.MovementMode;
import com.example.revolution.location.definition.SpawnMode;
import com.example.revolution.location.definition.SpawnRealmScope;
import com.example.revolution.location.service.LocationService;
import com.example.revolution.location.service.LocationService.HorizontalPoint;
import com.example.revolution.location.service.LocationService.ResolvedFixedSpawn;
import com.example.revolution.location.service.MobZoneLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class LocationFoundationTest {
    private LocationFoundationTest() {}

    public static void main(String[] args) {
        oneMobTypeCanUseManyPhysicalSlots();
        spawnAndMovementModesRemainIndependent();
        locationAreaIsCircular();
        randomPointsStayInsideLocationCircle();
        playerRealmsShareSpawnGeometry();
        realmZeroRequiresExplicitScope();
        mobZoneIdsRoundTripWorldLocationAndSlot();
        mobZonePopulationUsesStableHalfSplitAndDiskLayout();
        System.out.println("LocationFoundationTest passed");
    }

    private static void oneMobTypeCanUseManyPhysicalSlots() {
        LocationCatalog catalog = catalog();
        List<MobSpawnDefinition> spawns = catalog.spawnsForLocation(1);
        assertEquals(5, spawns.size(), "spawn slot count");
        for (int i = 0; i < 4; i++) assertEquals(1, spawns.get(i).getMobId(), "shared mob definition ID");
        assertTrue(spawns.get(0).getSpawnId() != spawns.get(1).getSpawnId(), "each physical mob slot needs its own spawn ID");
    }

    private static void spawnAndMovementModesRemainIndependent() {
        LocationCatalog catalog = catalog();
        assertModes(catalog.spawn(1, 1), SpawnMode.FIXED_POINT, MovementMode.STATIONARY);
        assertModes(catalog.spawn(1, 2), SpawnMode.FIXED_POINT, MovementMode.ROAMING);
        assertModes(catalog.spawn(1, 3), SpawnMode.RANDOM_AREA, MovementMode.STATIONARY);
        assertModes(catalog.spawn(1, 4), SpawnMode.RANDOM_AREA, MovementMode.ROAMING);
    }

    private static void locationAreaIsCircular() {
        LocationService service = new LocationService(catalog());
        assertTrue(service.isInsideLocation(1, 0.0D, 0.0D), "center is inside");
        assertTrue(service.isInsideLocation(1, 5.0D, 0.0D), "radius edge is inside");
        assertTrue(!service.isInsideLocation(1, 5.0D, 5.0D), "square corner outside circle must be rejected");
        assertTrue(!service.isBeyondLocation(1, 6.9D, 0.0D, 2.0D), "hard return margin allows up to radius + margin");
        assertTrue(service.isBeyondLocation(1, 7.1D, 0.0D, 2.0D), "mob beyond radius + margin is outside hard boundary");
    }

    private static void randomPointsStayInsideLocationCircle() {
        LocationService service = new LocationService(catalog());
        Random random = new Random(135L);
        for (int i = 0; i < 1000; i++) {
            HorizontalPoint point = service.randomPoint(1, random);
            assertTrue(service.isInsideLocation(1, point.getX(), point.getZ()), "random point must remain inside circle");
        }
    }

    private static void playerRealmsShareSpawnGeometry() {
        LocationService service = new LocationService(catalog());
        MobSpawnDefinition spawn = service.requireSpawn(1, 2);
        ResolvedFixedSpawn realm1 = service.resolveFixedSpawn(spawn, 1);
        ResolvedFixedSpawn realm2 = service.resolveFixedSpawn(spawn, 2);
        ResolvedFixedSpawn realm3 = service.resolveFixedSpawn(spawn, 3);

        assertEquals(201, realm1.getDimensionId(), "Realm 1 dimension");
        assertEquals(202, realm2.getDimensionId(), "Realm 2 dimension");
        assertEquals(203, realm3.getDimensionId(), "Realm 3 dimension");
        assertEquals(realm1.getX(), realm2.getX(), "shared X");
        assertEquals(realm1.getY(), realm3.getY(), "shared Y");
        assertEquals(realm1.getZ(), realm3.getZ(), "shared Z");
        assertEquals(4, service.spawnsForRealm(1, 1).size(), "player Realm spawn count");
        assertEquals(4, service.spawnsForRealm(1, 2).size(), "player Realm spawn count");
        assertEquals(4, service.spawnsForRealm(1, 3).size(), "player Realm spawn count");
    }

    private static void realmZeroRequiresExplicitScope() {
        LocationService service = new LocationService(catalog());
        assertEquals(1, service.spawnsForRealm(1, 0).size(), "Realm 0 only gets explicitly scoped spawns");
        MobSpawnDefinition realmZero = service.requireSpawn(1, 5);
        ResolvedFixedSpawn resolved = service.resolveFixedSpawn(realmZero, 0);
        assertEquals(0, resolved.getRealmId(), "Realm 0 ID");
        assertEquals(0, resolved.getDimensionId(), "Realm 0 dimension");

        boolean rejected = false;
        try {
            service.resolveFixedSpawn(service.requireSpawn(1, 1), 0);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assertTrue(rejected, "player-Realm spawn must not auto-apply to Realm 0");
    }

    private static void mobZoneIdsRoundTripWorldLocationAndSlot() {
        int encoded = MobZoneIds.encode(17, 8);
        assertTrue(MobZoneIds.isMobZoneId(encoded), "encoded ID must be recognized as mob zone");
        assertEquals(17, MobZoneIds.worldLocationId(encoded), "world Location ID round-trip");
        assertEquals(8, MobZoneIds.mobSlotId(encoded), "mob slot ID round-trip");
        assertTrue(!MobZoneIds.isMobZoneId(17), "legacy Location IDs must remain distinct");
    }

    private static void mobZonePopulationUsesStableHalfSplitAndDiskLayout() {
        assertEquals(10, MobZoneLayout.fixedCount(20), "even fixed split");
        assertEquals(10, MobZoneLayout.randomCount(20), "even random split");
        assertEquals(11, MobZoneLayout.fixedCount(21), "odd fixed split gets extra mob");
        assertEquals(10, MobZoneLayout.randomCount(21), "odd random split");

        int fixedCount = MobZoneLayout.fixedCount(20);
        for (int i = 0; i < fixedCount; i++) {
            MobZoneLayout.HorizontalPoint point = MobZoneLayout.fixedPoint(i, fixedCount, 12.0D, -4.0D, 15.0D);
            double dx = point.getX() - 12.0D;
            double dz = point.getZ() + 4.0D;
            assertTrue(dx * dx + dz * dz < 15.0D * 15.0D, "fixed point must stay inside mob zone");
        }
    }

    private static LocationCatalog catalog() {
        LocationDefinition location = new LocationDefinition(1, "Location 1", 0.0D, 0.0D, 5.0D);
        return new LocationCatalog(
                Arrays.asList(location),
                Arrays.asList(
                        spawn(1, SpawnMode.FIXED_POINT, MovementMode.STATIONARY, SpawnRealmScope.PLAYER_REALMS, -2.0D, 64.0D, 0.0D),
                        spawn(2, SpawnMode.FIXED_POINT, MovementMode.ROAMING, SpawnRealmScope.PLAYER_REALMS, 0.0D, 64.0D, 0.0D),
                        spawn(3, SpawnMode.RANDOM_AREA, MovementMode.STATIONARY, SpawnRealmScope.PLAYER_REALMS, 2.0D, 64.0D, 0.0D),
                        spawn(4, SpawnMode.RANDOM_AREA, MovementMode.ROAMING, SpawnRealmScope.PLAYER_REALMS, 0.0D, 64.0D, 2.0D),
                        spawn(5, SpawnMode.FIXED_POINT, MovementMode.STATIONARY, SpawnRealmScope.REALM_0, 0.0D, 64.0D, -2.0D)));
    }

    private static MobSpawnDefinition spawn(
            int spawnId,
            SpawnMode spawnMode,
            MovementMode movementMode,
            SpawnRealmScope realmScope,
            double x,
            double y,
            double z) {
        return new MobSpawnDefinition(1, spawnId, 1, x, y, z, 0.0F, 0.0F, spawnMode, movementMode, realmScope);
    }

    private static void assertModes(MobSpawnDefinition spawn, SpawnMode spawnMode, MovementMode movementMode) {
        assertEquals(spawnMode, spawn.getSpawnMode(), "spawn mode");
        assertEquals(movementMode, spawn.getMovementMode(), "movement mode");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
