package com.example.revolution.realm.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Fixed Realm catalog for the v1.3.4 foundation. Location data is deliberately outside this catalog. */
public final class RealmCatalog {
    private static final RealmDefinition ADMIN_REALM = new RealmDefinition(0, 0, "Realm 0", 0, 64, 0);
    private static final List<RealmDefinition> REALMS;
    private static final Map<Integer, RealmDefinition> BY_REALM_ID;
    private static final Map<Integer, RealmDefinition> BY_DIMENSION_ID;

    static {
        List<RealmDefinition> realms = new ArrayList<>();
        realms.add(new RealmDefinition(1, 201, "Realm 1", 0, 64, 0));
        realms.add(new RealmDefinition(2, 202, "Realm 2", 0, 64, 0));
        realms.add(new RealmDefinition(3, 203, "Realm 3", 0, 64, 0));
        REALMS = Collections.unmodifiableList(realms);

        Map<Integer, RealmDefinition> byRealm = new HashMap<>();
        Map<Integer, RealmDefinition> byDimension = new HashMap<>();
        for (RealmDefinition realm : REALMS) {
            if (byRealm.put(realm.getRealmId(), realm) != null) throw new IllegalStateException("Duplicate realm ID");
            if (byDimension.put(realm.getDimensionId(), realm) != null) throw new IllegalStateException("Duplicate dimension ID");
        }
        BY_REALM_ID = Collections.unmodifiableMap(byRealm);
        BY_DIMENSION_ID = Collections.unmodifiableMap(byDimension);
    }

    private RealmCatalog() {}

    /** Player Realms only. Realm 0 is intentionally excluded from balancing and Forge dimension registration. */
    public static List<RealmDefinition> all() { return REALMS; }
    public static RealmDefinition adminRealm() { return ADMIN_REALM; }
    public static RealmDefinition byRealmId(int realmId) { return realmId == 0 ? ADMIN_REALM : BY_REALM_ID.get(realmId); }
    public static RealmDefinition byDimensionId(int dimensionId) { return dimensionId == 0 ? ADMIN_REALM : BY_DIMENSION_ID.get(dimensionId); }
    public static boolean isRealmDimension(int dimensionId) { return BY_DIMENSION_ID.containsKey(dimensionId); }

    /** Selects randomly among all player Realms tied for the minimum current population. */
    public static RealmDefinition leastPopulated(Map<Integer, Integer> populationByDimension) {
        List<RealmDefinition> candidates = new ArrayList<>();
        int minimumPopulation = Integer.MAX_VALUE;
        for (RealmDefinition realm : REALMS) {
            Integer value = populationByDimension.get(realm.getDimensionId());
            int population = value == null ? 0 : Math.max(0, value);
            if (population < minimumPopulation) {
                candidates.clear();
                candidates.add(realm);
                minimumPopulation = population;
            } else if (population == minimumPopulation) {
                candidates.add(realm);
            }
        }
        if (candidates.isEmpty()) throw new IllegalStateException("Realm catalog is empty");
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }
}
