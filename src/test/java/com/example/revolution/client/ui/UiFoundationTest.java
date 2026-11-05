package com.example.revolution.client.ui;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UiFoundationTest {
    private final UiAvailabilityResolver resolver = new UiAvailabilityResolver();

    @Test void stateSemantics() {
        assertTrue(UiAvailability.AVAILABLE.isVisible());
        assertTrue(UiAvailability.AVAILABLE.isInteractive());
        assertTrue(UiAvailability.VISIBLE_LOCKED.isVisible());
        assertFalse(UiAvailability.VISIBLE_LOCKED.isInteractive());
        assertFalse(UiAvailability.HIDDEN.isVisible());
        assertFalse(UiAvailability.HIDDEN.isInteractive());
    }

    @Test void serverControlsVisibilityAndPermanentUnlocks() {
        assertEquals(UiAvailability.HIDDEN,
                resolver.resolve(UiAvailability.AVAILABLE, false, true, true));
        assertEquals(UiAvailability.VISIBLE_LOCKED,
                resolver.resolve(UiAvailability.AVAILABLE, true, false, true));
        assertEquals(UiAvailability.AVAILABLE,
                resolver.resolve(UiAvailability.AVAILABLE, true, true, true));
        assertEquals(UiAvailability.VISIBLE_LOCKED,
                resolver.resolve(UiAvailability.AVAILABLE, true, true, false));
    }

    @Test void parentGateAlwaysWins() {
        assertEquals(UiAvailability.HIDDEN,
                resolver.resolve(UiAvailability.HIDDEN, true, true, true));
        assertEquals(UiAvailability.VISIBLE_LOCKED,
                resolver.resolve(UiAvailability.VISIBLE_LOCKED, true, true, true));
        assertEquals(UiAvailability.HIDDEN,
                resolver.resolve(UiAvailability.VISIBLE_LOCKED, false, true, true));
        assertEquals(UiAvailability.HIDDEN,
                resolver.resolve(UiAvailability.HIDDEN, true, false, false));
    }

    @Test void sectionIdsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (UiMenuSection section : UiMenuSection.values()) {
            assertTrue(ids.add(section.id()));
            assertEquals(section, UiMenuSection.fromId(section.id()));
        }
        assertEquals(5, ids.size());
        assertThrows(IllegalArgumentException.class, () -> UiMenuSection.fromId("unknown"));
    }
}
