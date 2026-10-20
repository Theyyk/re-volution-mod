package com.example.revolution.realm.definition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class RealmCatalogTest {
    private RealmCatalogTest() {}

    public static void main(String[] args) {
        catalogHasThreeDistinctPlayerRealms();
        adminRealmZeroIsSeparateFromPlayerCatalog();
        leastPopulatedRealmIsSelected();
        System.out.println("RealmCatalogTest passed");
    }

    private static void catalogHasThreeDistinctPlayerRealms() {
        assertEquals(3, RealmCatalog.all().size(), "player realm count");
        Set<Integer> realmIds = new HashSet<>();
        Set<Integer> dimensionIds = new HashSet<>();
        for (RealmDefinition realm : RealmCatalog.all()) {
            assertTrue(realmIds.add(realm.getRealmId()), "realm IDs must be unique");
            assertTrue(dimensionIds.add(realm.getDimensionId()), "dimension IDs must be unique");
            assertTrue(realm.getRealmId() != realm.getDimensionId(), "gameplay Realm ID must differ from Forge dimension ID");
        }
        for (Integer realmId : realmIds) assertTrue(!dimensionIds.contains(realmId), "Realm IDs and dimension IDs must be separate namespaces");
    }

    private static void adminRealmZeroIsSeparateFromPlayerCatalog() {
        RealmDefinition realm = RealmCatalog.byRealmId(0);
        assertTrue(realm != null, "Realm 0 must be resolvable for admin join");
        assertEquals(0, realm.getRealmId(), "Realm 0 ID");
        assertEquals(0, realm.getDimensionId(), "Realm 0 dimension");
        assertEquals(realm, RealmCatalog.byDimensionId(0), "dimension 0 lookup");
        assertTrue(!RealmCatalog.all().contains(realm), "Realm 0 must not be in player balancing catalog");
        assertTrue(!RealmCatalog.isRealmDimension(0), "Realm 0 must not be treated as a player Realm dimension");
    }

    private static void leastPopulatedRealmIsSelected() {
        Map<Integer, Integer> population = new HashMap<>();
        population.put(201, 4);
        population.put(202, 1);
        population.put(203, 3);
        population.put(0, 0);
        assertEquals(2, RealmCatalog.leastPopulated(population).getRealmId(), "least-populated player Realm");
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
