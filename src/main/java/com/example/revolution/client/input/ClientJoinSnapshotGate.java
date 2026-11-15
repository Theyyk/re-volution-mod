package com.example.revolution.client.input;

/**
 * Requests initial server-authored UI state once the client has both a live
 * connection and a player. A disconnect re-arms the gate for reconnect.
 */
final class ClientJoinSnapshotGate {
    private boolean requested;

    boolean shouldRequest(boolean connected, boolean playerReady) {
        if (!connected || !playerReady) {
            requested = false;
            return false;
        }
        if (requested) return false;
        requested = true;
        return true;
    }
}
