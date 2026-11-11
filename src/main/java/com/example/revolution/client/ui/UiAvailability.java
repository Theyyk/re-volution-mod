package com.example.revolution.client.ui;

/**
 * Presentation state only. Server-side gameplay permission checks remain authoritative.
 */
public enum UiAvailability {
    AVAILABLE,
    VISIBLE_LOCKED,
    HIDDEN;

    public boolean isVisible() {
        return this != HIDDEN;
    }

    public boolean isInteractive() {
        return this == AVAILABLE;
    }

    /** A parent never grants an action more availability than it has itself. */
    public static UiAvailability restrict(UiAvailability parent, UiAvailability child) {
        if (parent == null || child == null) throw new IllegalArgumentException("UI states must be non-null");
        if (parent == HIDDEN || child == HIDDEN) return HIDDEN;
        if (parent == VISIBLE_LOCKED || child == VISIBLE_LOCKED) return VISIBLE_LOCKED;
        return AVAILABLE;
    }
}
