package com.example.revolution.admin.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable projection of a persisted player, enriched with live state when online. */
public final class AdminPlayerSnapshot {
    public final String uuid;
    public final String name;
    public final boolean online;
    public final int dimension;
    public final double x;
    public final double y;
    public final double z;
    public final int runeOwned;
    public final int runeNormal;
    public final int runeImproved;
    public final int runeAdvanced;
    public final Map<String, String> balances;

    public AdminPlayerSnapshot(String uuid, String name, boolean online, int dimension, double x, double y, double z,
                               int runeOwned, int runeNormal, int runeImproved, int runeAdvanced,
                               Map<String, String> balances) {
        this.uuid = uuid;
        this.name = name;
        this.online = online;
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
