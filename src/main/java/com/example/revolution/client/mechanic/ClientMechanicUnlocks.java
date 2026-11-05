package com.example.revolution.client.mechanic;

import com.example.revolution.network.MechanicUnlockSnapshotPacket;
import com.example.revolution.client.ui.UiAvailability;
import com.example.revolution.client.ui.UiAvailabilityResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientMechanicUnlocks {
    private static final List<MechanicUnlockSnapshotPacket.Entry> ENTRIES = new ArrayList<>();
    private static boolean snapshotReceived;
    private static final UiAvailabilityResolver UI_STATES = new UiAvailabilityResolver();
    private static boolean readyHighlight;
    private static String zbtStatus = "draft";
    private ClientMechanicUnlocks() {}
    public static synchronized void apply(MechanicUnlockSnapshotPacket packet) {
        if (packet == null) throw new IllegalArgumentException("unlock snapshot is required");
        ENTRIES.clear(); ENTRIES.addAll(packet.getEntries()); readyHighlight = packet.isReadyHighlight(); zbtStatus = packet.getZbtStatus(); snapshotReceived = true;
    }
    public static synchronized List<MechanicUnlockSnapshotPacket.Entry> entries() { return Collections.unmodifiableList(new ArrayList<>(ENTRIES)); }
    public static synchronized boolean readyHighlight() { return readyHighlight; }
    public static synchronized String zbtStatus() { return zbtStatus; }
    /**
     * Missing visible entries are hidden by the server's Mechanic Unlock visibility policy.
     * Before the initial sync, gated sections are hidden, never assumed unlocked.
     */
    public static synchronized UiAvailability uiState(String id, UiAvailability parent) {
        if (!snapshotReceived || id == null) return UiAvailability.restrict(parent, UiAvailability.HIDDEN);
        for (MechanicUnlockSnapshotPacket.Entry entry : ENTRIES) {
            if (id.equals(entry.unlockId)) {
                return UI_STATES.resolve(parent, true, entry.unlocked, true);
            }
        }
        return UI_STATES.resolve(parent, false, false, false);
    }

    public static synchronized void reset() {
        ENTRIES.clear(); readyHighlight = false; zbtStatus = "draft"; snapshotReceived = false;
    }

    public static synchronized boolean isUnlocked(String id) { for (MechanicUnlockSnapshotPacket.Entry e : ENTRIES) if (id.equals(e.unlockId)) return e.unlocked; return false; }
}
