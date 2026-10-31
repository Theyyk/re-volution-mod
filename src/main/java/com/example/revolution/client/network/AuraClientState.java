package com.example.revolution.client.network;

import com.example.revolution.network.AuraSnapshotPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-side read-only projection. Server snapshots are the only mutation source. */
public final class AuraClientState {
    private static final List<AuraSnapshotPacket.Entry> ENTRIES = new ArrayList<>();
    private static final Map<String, Integer> CRAFT_COSTS = new HashMap<>();
    private static final Map<String, Integer> DISMANTLE_RETURNS = new HashMap<>();
    private static int auraKeys;
    private static int crystals;
    private static int keyCrystalPrice;
    private static long totalChestOpens;
    private static long totalChestAuraDrops;
    private static boolean skipAnimation;
    private static boolean featureEnabled;
    private static boolean openRequested;

    private AuraClientState() {}
    public static synchronized void update(AuraSnapshotPacket packet) {
        ENTRIES.clear(); ENTRIES.addAll(packet.getEntries()); auraKeys = packet.getAuraKeys(); crystals = packet.getCrystals();
        keyCrystalPrice = packet.getKeyCrystalPrice(); totalChestOpens = packet.getTotalChestOpens(); totalChestAuraDrops = packet.getTotalChestAuraDrops();
        skipAnimation = packet.isSkipOpeningAnimation(); featureEnabled = packet.isFeatureEnabled();
        CRAFT_COSTS.clear(); DISMANTLE_RETURNS.clear();
        for (String rule : packet.getRarityRules().split("\\|")) {
            String[] parts = rule.split(":"); if (parts.length != 3) continue;
            try { CRAFT_COSTS.put(parts[0], Integer.parseInt(parts[1])); DISMANTLE_RETURNS.put(parts[0], Integer.parseInt(parts[2])); }
            catch (NumberFormatException ignored) { }
        }
    }
    public static synchronized List<AuraSnapshotPacket.Entry> entries() { return Collections.unmodifiableList(new ArrayList<>(ENTRIES)); }
    public static synchronized int auraKeys() { return auraKeys; }
    public static synchronized int crystals() { return crystals; }
    public static synchronized int keyCrystalPrice() { return keyCrystalPrice; }
    public static synchronized long totalChestOpens() { return totalChestOpens; }
    public static synchronized long totalChestAuraDrops() { return totalChestAuraDrops; }
    public static synchronized boolean skipAnimation() { return skipAnimation; }
    public static synchronized boolean featureEnabled() { return featureEnabled; }
    public static synchronized int craftCost(String rarity) { Integer value = CRAFT_COSTS.get(rarity); return value == null ? 0 : Math.max(0, value); }
    public static synchronized int dismantleReturn(String rarity) { Integer value = DISMANTLE_RETURNS.get(rarity); return value == null ? 0 : Math.max(0, value); }
    public static synchronized void requestOpen() { openRequested = true; }
    public static synchronized boolean consumeOpenRequested() { boolean value = openRequested; openRequested = false; return value; }
}
