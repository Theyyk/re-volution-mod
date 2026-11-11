package com.example.revolution.client;

import com.example.revolution.network.PetSnapshotPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PetContextSlotLayoutTest {
    @Test void exactlyTenCellsHaveNonOverlappingVisualAndClickBoundsAtSeveralScales() {
        for (int availableWidth : new int[]{180, 350, 594, 930}) {
            PetContextSlotLayout layout = new PetContextSlotLayout(100, availableWidth, 80);
            for (int i = 0; i < 10; i++) {
                int centerX = layout.x(i) + layout.width() / 2;
                int centerY = layout.y(i) + layout.cellHeight() / 2;
                assertEquals(i, layout.hit(centerX, centerY));
                assertEquals(i, layout.hit(layout.x(i), layout.y(i)));
                assertEquals(i, layout.hit(layout.x(i) + layout.width() - 1,
                        layout.y(i) + layout.cellHeight() - 1));
                assertTrue(layout.x(i) >= 100);
                assertTrue(layout.x(i) + layout.width() <= 100 + availableWidth);
                assertTrue(layout.y(i) >= 80);
                assertTrue(layout.y(i) + layout.cellHeight() <= 80 + layout.height());
                for (int j = 0; j < i; j++) {
                    boolean disjointX = layout.x(i) >= layout.x(j) + layout.width()
                            || layout.x(j) >= layout.x(i) + layout.width();
                    boolean disjointY = layout.y(i) >= layout.y(j) + layout.cellHeight()
                            || layout.y(j) >= layout.y(i) + layout.cellHeight();
                    assertTrue(disjointX || disjointY);
                }
            }
            assertEquals(-1, layout.hit(99, 80));
            assertEquals(-1, layout.hit(100, layout.y(0) - 1));
            assertEquals(-1, layout.hit(100, layout.y(9) + layout.cellHeight()));
            assertEquals(-1, layout.hit(layout.x(0), layout.y(0) + layout.cellHeight()));
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
