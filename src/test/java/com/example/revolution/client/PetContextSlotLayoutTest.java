package com.example.revolution.client;

import com.example.revolution.network.PetSnapshotPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PetContextSlotLayoutTest {
    @Test void exactlyTenCellsHaveNonOverlappingVisualAndClickBounds() {
        for (int availableWidth : new int[]{180, 350, 594, 930}) {
            PetContextSlotLayout layout = new PetContextSlotLayout(100, availableWidth, 80);
            for (int i = 0; i < 10; i++) {
                int centerX = layout.x(i) + layout.width() / 2;
                int centerY = layout.y() + layout.height() / 2;
                assertEquals(i, layout.hit(centerX, centerY));
                assertEquals(i, layout.hit(layout.x(i), layout.y()));
                assertEquals(i, layout.hit(layout.x(i) + layout.width() - 1, layout.y() + layout.height() - 1));
                assertTrue(layout.x(i) >= 100);
                assertTrue(layout.x(i) + layout.width() <= 100 + availableWidth);
                if (i > 0) assertTrue(layout.x(i) >= layout.x(i - 1) + layout.width());
            }
            assertEquals(-1, layout.hit(99, 80));
            assertEquals(-1, layout.hit(100, layout.y() - 1));
            assertEquals(-1, layout.hit(100, layout.y() + layout.height()));
        }
    }

    @Test void unlocksFollowExistingEightNormalPlusTwoPremiumLimits() {
        for (int i = 0; i < 10; i++) {
            assertFalse(PetContextSlotLayout.unlocked(i, 0, 0));
            assertTrue(PetContextSlotLayout.unlocked(i, 8, 2));
        }
        assertTrue(PetContextSlotLayout.unlocked(0, 1, 0));
        assertFalse(PetContextSlotLayout.unlocked(1, 1, 0));
        assertFalse(PetContextSlotLayout.unlocked(8, 8, 0));
        assertTrue(PetContextSlotLayout.unlocked(8, 0, 1));
        assertFalse(PetContextSlotLayout.unlocked(9, 0, 1));
        assertFalse(PetContextSlotLayout.unlocked(10, 8, 2));
        assertFalse(PetContextSlotLayout.unlocked(-1, 8, 2));
    }

    @Test void occupiedCellsReflectOnlyAuthoritativeServerAssignment() {
        List<PetSnapshotPacket.Entry> entries = new ArrayList<>();
        assertNull(PetContextSlotLayout.assigned(entries, 0));
        entries.add(pet("pet_02", true, 8));
        entries.add(pet("pet_01", true, 0));
        entries.add(pet("pet_03", false, 1)); // Unselected catalog item is not on the board.
        entries.add(pet("pet_04", true, 12)); // Malformed/out-of-range index is ignored.
        assertEquals("pet_01", PetContextSlotLayout.assigned(entries, 0).petId);
        assertEquals("pet_02", PetContextSlotLayout.assigned(entries, 8).petId);
        assertNull(PetContextSlotLayout.assigned(entries, 1));
        assertNull(PetContextSlotLayout.assigned(entries, 9));
        assertNull(PetContextSlotLayout.assigned(entries, 10));
        assertNull(PetContextSlotLayout.assigned(Collections.emptyList(), -1));
    }

    private static PetSnapshotPacket.Entry pet(String id, boolean selected, int index) {
        return new PetSnapshotPacket.Entry(id, "ORDINARY", "1", "0", "100",
                false, true, selected, index >= 8, 0L, "0", "", index);
    }
}
