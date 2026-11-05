package com.example.revolution.client.ui;

/**
 * Pure presentation adapter for Balance Core / Mechanic Unlock snapshots.
 * Visibility and permanent unlocking come ONLY from server-authoritative rules.
 * Local action readiness may further disable an action, but never grant access.
 */
public final class UiAvailabilityResolver {
    /**
     * @param parent resolved availability of the parent screen/section
     * @param serverVisible whether the server included this feature in its visible snapshot
     * @param serverUnlocked permanent server-authoritative unlock
     * @param actionReady temporary condition: sufficient resources, cooldown, etc.
     */
    public UiAvailability resolve(UiAvailability parent, boolean serverVisible,
                                  boolean serverUnlocked, boolean actionReady) {
        if (parent == null) throw new IllegalArgumentException("parent UI state is required");
        UiAvailability own = !serverVisible ? UiAvailability.HIDDEN
                : serverUnlocked && actionReady ? UiAvailability.AVAILABLE
                : UiAvailability.VISIBLE_LOCKED;
        return UiAvailability.restrict(parent, own);
    }
}
