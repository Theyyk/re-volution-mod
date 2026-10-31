package com.example.revolution.client.network;

import com.example.revolution.network.AuraSnapshotPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Client-side read-only projection. Server snapshots are the only mutation source. */
public final class AuraClientState {
    private static final List<AuraSnapshotPacket.Entry> ENTRIES = new ArrayList<>();
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
    }
    public static synchronized List<AuraSnapshotPacket.Entry> entries() { return Collections.unmodifiableList(new ArrayList<>(ENTRIES)); }
    public static synchronized int auraKeys() { return auraKeys; }
    public static synchronized int crystals() { return crystals; }
    public static synchronized int keyCrystalPrice() { return keyCrystalPrice; }
    public static synchronized long totalChestOpens() { return totalChestOpens; }
    public static synchronized long totalChestAuraDrops() { return totalChestAuraDrops; }
    public static synchronized boolean skipAnimation() { return skipAnimation; }
    public static synchronized boolean featureEnabled() { return featureEnabled; }
    public static synchronized void requestOpen() { openRequested = true; }
    public static synchronized boolean consumeOpenRequested() { boolean value = openRequested; openRequested = false; return value; }
}
