package com.example.revolution.client.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure, UI-only availability resolution; may be used for menus and nested actions.
 * A presentation preference never overrides server unlocks or temporary action rules.
 * Unspecified preferences default to AVAILABLE (legacy visibility preserved).
 */
public final class UiAvailabilityResolver {
    private final Map<String, UiAvailability> presentation;

    public UiAvailabilityResolver(Map<String, UiAvailability> presentation) {
        if (presentation == null) throw new IllegalArgumentException("presentation is required");
        Map<String, UiAvailability> copy = new LinkedHashMap<>();
        for (Map.Entry<String, UiAvailability> entry : presentation.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty() || entry.getValue() == null)
                throw new IllegalArgumentException("UI feature identifiers and states must be non-null/non-empty");
            copy.put(entry.getKey(), entry.getValue());
        }
        this.presentation = Collections.unmodifiableMap(copy);
    }

    public UiAvailability preference(String featureId) {
        if (featureId == null || featureId.trim().isEmpty())
            throw new IllegalArgumentException("UI feature identifier is required");
        UiAvailability configured = presentation.get(featureId);
        return configured == null ? UiAvailability.AVAILABLE : configured;
    }

    /**
     * @param parent resolved state of the containing screen/section
     * @param implemented false for a feature without a working implementation
     * @param serverUnlocked authoritative unlock snapshot for this feature
     * @param actionReady temporary local action feasibility (cost, cooldown, limits)
     */
    public UiAvailability resolve(String featureId, UiAvailability parent, boolean implemented,
                                  boolean serverUnlocked, boolean actionReady) {
        if (parent == null) throw new IllegalArgumentException("parent UI state is required");
        if (!implemented) return UiAvailability.HIDDEN;
        UiAvailability own = preference(featureId);
        if (!serverUnlocked || !actionReady) {
            own = UiAvailability.restrict(own, UiAvailability.VISIBLE_LOCKED);
        }
        return UiAvailability.restrict(parent, own);
    }

    public Map<String, UiAvailability> snapshot() {
        return presentation;
    }
}
