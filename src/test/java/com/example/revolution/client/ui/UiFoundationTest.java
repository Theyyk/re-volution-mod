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
        Set<Integer> buttonIds = new HashSet<>();
        for (UiMenuSection section : UiMenuSection.values()) {
            assertTrue(buttonIds.add(section.buttonId()), "duplicate GUI button ID");
            assertEquals(section, UiMenuSection.fromButtonId(section.buttonId()));
        }
        assertEquals(200, UiMenuSection.RUNES.buttonId());
        assertEquals(4, UiMenuSection.auxiliaryTabs().length);
        for (UiMenuSection tab : UiMenuSection.auxiliaryTabs()) {
            // The original screen reserves IDs 0-299.
            assertTrue(tab.buttonId() >= 300);
            assertNotNull(tab.shortLabel());
        }
        assertNull(UiMenuSection.fromButtonId(9999));
        assertEquals("UNIQUE_RUNE_COLLECTIONS", UiMenuSection.UNIQUE_RUNES.mechanicUnlockId());
        assertEquals("ACHIEVEMENTS", UiMenuSection.ACHIEVEMENTS.mechanicUnlockId());
        assertNull(UiMenuSection.PETS.mechanicUnlockId());
        assertNull(UiMenuSection.MECHANIC_UNLOCKS.mechanicUnlockId());
        assertThrows(IllegalArgumentException.class, () -> UiMenuSection.fromId("unknown"));
    }
}
