package com.example.revolution.client.ui;

/** Read-only Aura slot presentation; the server's single active Aura remains authoritative. */
public enum AuraSlotState {
    LOCKED, EMPTY, ACTIVE;

    public static AuraSlotState resolve(boolean available, boolean hasActiveAura) {
        if (!available) return LOCKED;
        return hasActiveAura ? ACTIVE : EMPTY;
    }
}
