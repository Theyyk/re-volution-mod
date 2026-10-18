package com.example.customguimod.client;
/** Keeps purchase controls hidden only during an explicit rune collection synchronization. */
final class RuneCollectionSyncState {
    private static final long SETTLE_DELAY_MS = 250L;

    private static boolean syncing;
    private static boolean receivedPacket;
    private static long lastPacketAt;

    private RuneCollectionSyncState() {}

    static void beginLoad() {
        syncing = true;
        receivedPacket = false;
        lastPacketAt = 0L;
    }

    static void notePacket() {
        if (!syncing) return;
        receivedPacket = true;
        lastPacketAt = System.currentTimeMillis();
    }

    static void completeLoad() {
        syncing = false;
        receivedPacket = false;
        lastPacketAt = 0L;
    }

    static boolean isLoading() {
        if (!syncing) return false;
        if (!receivedPacket) return true;
        if (System.currentTimeMillis() - lastPacketAt < SETTLE_DELAY_MS) return true;

        syncing = false;
        receivedPacket = false;
        lastPacketAt = 0L;
        return false;
    }
}
