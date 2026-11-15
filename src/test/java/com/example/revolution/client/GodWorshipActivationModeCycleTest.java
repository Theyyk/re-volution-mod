package com.example.revolution.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** BC-01: the client must send the canonical server enum values, never translated labels. */
final class GodWorshipActivationModeCycleTest {
    @Test
    void cyclesThroughAllThreeWireModesAndReturnsToManual() {
        String manual = "MANUAL";
        String auto = GodWorshipGuiScreen.nextMode(manual);
        String linked = GodWorshipGuiScreen.nextMode(auto);
        String backToManual = GodWorshipGuiScreen.nextMode(linked);

        assertEquals("AUTO_WHEN_READY", auto);
        assertEquals("LINKED_TO_GOD", linked);
        assertEquals("MANUAL", backToManual);
        assertEquals(auto, GodWorshipGuiScreen.nextMode(backToManual));
    }
}
