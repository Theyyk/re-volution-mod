package com.example.revolution.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class UniqueRollbackConfirmationTest {
    @Test void requiresSameRuneAndCollection() {
        UniqueRollbackConfirmation c = new UniqueRollbackConfirmation();
        assertFalse(c.confirmOrArm("CRYSTAL_COLLECTION", "crystal_01"));
        assertTrue(c.isArmed());
        assertFalse(c.confirmOrArm("CRYSTAL_COLLECTION", "crystal_02"),
                "different rune must re-arm without rolling back");
        assertTrue(c.isArmed());
        assertTrue(c.confirmOrArm("CRYSTAL_COLLECTION", "crystal_02"));
        assertFalse(c.isArmed());
        assertFalse(c.confirmOrArm("CRYSTAL_COLLECTION", "crystal_02"),
                "third gesture must start a fresh confirmation");
    }

    @Test void collectionSwitchAndCancelBreakConfirmation() {
        UniqueRollbackConfirmation c = new UniqueRollbackConfirmation();
        assertFalse(c.confirmOrArm("CRYSTAL_COLLECTION", "crystal_01"));
        assertFalse(c.confirmOrArm("DAILY_COLLECTION", "daily_01"));
        assertTrue(c.confirmOrArm("DAILY_COLLECTION", "daily_01"));
        assertFalse(c.confirmOrArm("CRYSTAL_COLLECTION", "crystal_01"));
        c.cancel();
        assertFalse(c.confirmOrArm("CRYSTAL_COLLECTION", "crystal_01"));
    }

    @Test void invalidSelectionCannotConfirm() {
        UniqueRollbackConfirmation c = new UniqueRollbackConfirmation();
        assertFalse(c.confirmOrArm("CRYSTAL_COLLECTION", "crystal_01"));
        assertFalse(c.confirmOrArm("", "crystal_01"));
        assertFalse(c.isArmed());
        assertFalse(c.confirmOrArm("CRYSTAL_COLLECTION", null));
    }
}
