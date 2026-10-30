package com.example.revolution.client;

import com.example.revolution.network.PetSnapshotPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only client mirror of the last server Pet snapshot. */
public final class ClientPets {
    private static int normalSlots;
    private static int premiumSlots;
    private static String nextNormalSlotCost = "0";
    private static List<PetSnapshotPacket.Entry> entries = Collections.emptyList();
    private ClientPets() {}

    public static synchronized void accept(PetSnapshotPacket packet) {
        normalSlots = packet.getNormalSlots(); premiumSlots = packet.getPremiumSlots();
        nextNormalSlotCost = packet.getNextNormalSlotCost(); entries = new ArrayList<>(packet.getEntries());
    }
    public static synchronized int normalSlots() { return normalSlots; }
    public static synchronized int premiumSlots() { return premiumSlots; }
    public static synchronized String nextNormalSlotCost() { return nextNormalSlotCost; }
    public static synchronized List<PetSnapshotPacket.Entry> entries() { return new ArrayList<>(entries); }
}
