package com.example.revolution.client.mechanic;

import com.example.revolution.network.MechanicUnlockSnapshotPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientMechanicUnlocks {
    private static final List<MechanicUnlockSnapshotPacket.Entry> ENTRIES = new ArrayList<>();
    private static boolean readyHighlight;
    private static String zbtStatus = "draft";
    private ClientMechanicUnlocks() {}
    public static synchronized void apply(MechanicUnlockSnapshotPacket packet) {
        ENTRIES.clear(); ENTRIES.addAll(packet.getEntries()); readyHighlight = packet.isReadyHighlight(); zbtStatus = packet.getZbtStatus();
    }
    public static synchronized List<MechanicUnlockSnapshotPacket.Entry> entries() { return Collections.unmodifiableList(new ArrayList<>(ENTRIES)); }
    public static synchronized boolean readyHighlight() { return readyHighlight; }
    public static synchronized String zbtStatus() { return zbtStatus; }
    public static synchronized boolean isUnlocked(String id) { for (MechanicUnlockSnapshotPacket.Entry e : ENTRIES) if (id.equals(e.unlockId)) return e.unlocked; return false; }
}
