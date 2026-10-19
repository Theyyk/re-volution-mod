package com.example.revolution.admin.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable projection of one live player for external administration. */
public final class AdminPlayerSnapshot {
    public final String uuid;
    public final String name;
    public final int dimension;
    public final double x;
    public final double y;
    public final double z;
    public final int runeOwned;
    public final int runeNormal;
    public final int runeImproved;
    public final int runeAdvanced;
    public final Map<String, String> balances;

    public AdminPlayerSnapshot(String uuid, String name, int dimension, double x, double y, double z,
                               int runeOwned, int runeNormal, int runeImproved, int runeAdvanced,
                               Map<String, String> balances) {
        this.uuid = uuid;
        this.name = name;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.runeOwned = runeOwned;
        this.runeNormal = runeNormal;
        this.runeImproved = runeImproved;
        this.runeAdvanced = runeAdvanced;
        this.balances = Collections.unmodifiableMap(new LinkedHashMap<>(balances));
    }
}
