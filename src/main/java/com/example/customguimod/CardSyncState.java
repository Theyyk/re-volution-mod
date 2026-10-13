package com.example.customguimod;

/** Keeps purchase controls hidden while the rune collection is being synchronized. */
final class CardSyncState {
    private static final long SETTLE_DELAY_MS = 250L;

    private static boolean waitingForServer = true;
    private static long lastPacketAt;

    private CardSyncState() {}

    static void beginLoad() {
        waitingForServer = true;
        lastPacketAt = 0L;
    }

    static void notePacket() {
        waitingForServer = false;
        lastPacketAt = System.currentTimeMillis();
    }

    static boolean isLoading() {
        return waitingForServer
                || System.currentTimeMillis() - lastPacketAt < SETTLE_DELAY_MS;
    }
}
