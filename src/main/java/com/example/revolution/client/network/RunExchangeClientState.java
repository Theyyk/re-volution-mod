package com.example.revolution.client.network;

import com.example.revolution.network.RunExchangeSnapshotPacket;

/** Ephemeral server-only Run Exchange projection; no local economy calculations. */
public final class RunExchangeClientState {
    private static long sequence = System.currentTimeMillis();
    private static long latestIssued;
    private static RunExchangeSnapshotPacket snapshot;
    private static boolean openRequested;
    private static boolean pending;

    private RunExchangeClientState() {}

    public static synchronized long nextRequestId() {
        latestIssued = ++sequence;
        pending = true;
        return latestIssued;
    }

    public static synchronized boolean apply(RunExchangeSnapshotPacket next) {
        if (next == null || next.requestId < latestIssued) return false;
        snapshot = next;
        pending = false;
        return true;
    }

    public static synchronized RunExchangeSnapshotPacket snapshot() { return snapshot; }
    public static synchronized boolean pending() { return pending; }
    public static synchronized void requestOpen() { openRequested = true; }
    public static synchronized boolean consumeOpenRequested() {
        boolean requested = openRequested;
        openRequested = false;
        return requested;
    }
    public static synchronized void reset() {
        snapshot = null;
        openRequested = false;
        pending = false;
        latestIssued = sequence;
    }
}
