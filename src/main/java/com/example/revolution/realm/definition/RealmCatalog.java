package com.example.revolution.realm.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Fixed Realm catalog for the v1.3.4 foundation. Location data is deliberately outside this catalog. */
public final class RealmCatalog {
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

    public static List<RealmDefinition> all() { return REALMS; }
    public static RealmDefinition byRealmId(int realmId) { return BY_REALM_ID.get(realmId); }
    public static RealmDefinition byDimensionId(int dimensionId) { return BY_DIMENSION_ID.get(dimensionId); }
    public static boolean isRealmDimension(int dimensionId) { return BY_DIMENSION_ID.containsKey(dimensionId); }

    /**
     * Selects the least-populated Realm. Equal population currently falls back to catalog order so
     * tie policy stays isolated from the rest of the runtime and can be changed later.
     */
    public static RealmDefinition leastPopulated(Map<Integer, Integer> populationByDimension) {
        RealmDefinition selected = null;
        int selectedPopulation = Integer.MAX_VALUE;
        for (RealmDefinition realm : REALMS) {
            Integer value = populationByDimension.get(realm.getDimensionId());
            int population = value == null ? 0 : Math.max(0, value);
            if (selected == null || population < selectedPopulation) {
                selected = realm;
                selectedPopulation = population;
            }
        }
        if (selected == null) throw new IllegalStateException("Realm catalog is empty");
        return selected;
    }
}
