package com.example.revolution.client.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UiFoundationTest {
    @Test
    void allStatesAreExplicitAndSafe() {
        assertTrue(UiAvailability.AVAILABLE.isVisible());
        assertTrue(UiAvailability.AVAILABLE.isInteractive());
        assertTrue(UiAvailability.VISIBLE_LOCKED.isVisible());
        assertFalse(UiAvailability.VISIBLE_LOCKED.isInteractive());
        assertFalse(UiAvailability.HIDDEN.isVisible());
        assertFalse(UiAvailability.HIDDEN.isInteractive());
    }

    @Test
    void parentCannotBeBypassedByChild() {
        assertEquals(UiAvailability.HIDDEN,
                UiAvailability.restrict(UiAvailability.HIDDEN, UiAvailability.AVAILABLE));
        assertEquals(UiAvailability.VISIBLE_LOCKED,
                UiAvailability.restrict(UiAvailability.VISIBLE_LOCKED, UiAvailability.AVAILABLE));
        assertEquals(UiAvailability.HIDDEN,
                UiAvailability.restrict(UiAvailability.AVAILABLE, UiAvailability.HIDDEN));
    }

    @Test
    void presentationDoesNotGrantServerAccess() {
        UiAvailabilityResolver resolver = new UiAvailabilityResolver(Collections.emptyMap());
        assertEquals(UiAvailability.VISIBLE_LOCKED,
                resolver.resolve("gods.upgrade", UiAvailability.AVAILABLE, true, false, true));
        assertEquals(UiAvailability.VISIBLE_LOCKED,
                resolver.resolve("gods.upgrade", UiAvailability.AVAILABLE, true, true, false));
        assertEquals(UiAvailability.HIDDEN,
                resolver.resolve("unimplemented", UiAvailability.AVAILABLE, false, true, true));
    }

    @Test
    void localPreferenceAppliesToNestedFeatures() {
        Map<String, UiAvailability> config = new LinkedHashMap<>();
        config.put("gods.auto", UiAvailability.HIDDEN);
        config.put("gods.offerings", UiAvailability.VISIBLE_LOCKED);
        UiAvailabilityResolver resolver = new UiAvailabilityResolver(config);
        assertEquals(UiAvailability.HIDDEN,
                resolver.resolve("gods.auto", UiAvailability.AVAILABLE, true, true, true));
        assertEquals(UiAvailability.VISIBLE_LOCKED,
                resolver.resolve("gods.offerings", UiAvailability.AVAILABLE, true, true, true));
        assertEquals(UiAvailability.AVAILABLE,
                resolver.resolve("gods.charge", UiAvailability.AVAILABLE, true, true, true));
        assertEquals(UiAvailability.HIDDEN,
                resolver.resolve("gods.charge", UiAvailability.HIDDEN, true, true, true));
        assertThrows(UnsupportedOperationException.class,
                () -> resolver.snapshot().put("gods.test", UiAvailability.AVAILABLE));
    }

    @Test
    void mainMenuSectionIdsAreUniqueAndStable() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (UiMenuSection section : UiMenuSection.values()) {
            assertTrue(ids.add(section.id()));
            assertEquals(section, UiMenuSection.fromId(section.id()));
        }
        assertEquals(5, ids.size());
        assertEquals(UiMenuSection.RUNES, UiMenuSection.fromId("runes"));
        assertThrows(IllegalArgumentException.class, () -> UiMenuSection.fromId("unknown"));
    }
}
