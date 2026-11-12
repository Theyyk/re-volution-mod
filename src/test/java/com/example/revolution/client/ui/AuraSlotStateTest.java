package com.example.revolution.client.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AuraSlotStateTest {
    @Test void lockedTakesPriorityOverCachedActiveAura() {
        assertEquals(AuraSlotState.LOCKED, AuraSlotState.resolve(false, true));
        assertEquals(AuraSlotState.LOCKED, AuraSlotState.resolve(false, false));
    }

    @Test void emptyAndActiveAreServerProjectionOnly() {
        assertEquals(AuraSlotState.EMPTY, AuraSlotState.resolve(true, false));
        assertEquals(AuraSlotState.ACTIVE, AuraSlotState.resolve(true, true));
    }
}
