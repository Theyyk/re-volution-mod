package com.example.revolution.client.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AuraListLayoutTest {
    @Test void clickMatchesVisibleRowNotPreviousRow() {
        assertEquals(0, AuraListLayout.indexAt(120, 203, 100, 200, 220, 24, 6, 0, 12));
        assertEquals(1, AuraListLayout.indexAt(120, 224, 100, 200, 220, 24, 6, 0, 12));
        assertEquals(5, AuraListLayout.indexAt(120, 343, 100, 200, 220, 24, 6, 0, 12));
        assertEquals(-1, AuraListLayout.indexAt(120, 199, 100, 200, 220, 24, 6, 0, 12));
        assertEquals(-1, AuraListLayout.indexAt(120, 344, 100, 200, 220, 24, 6, 0, 12));
    }

    @Test void pagingRespectsBoundsAndPartialLastPage() {
        assertEquals(0, AuraListLayout.lastPage(0, 6));
        assertEquals(1, AuraListLayout.lastPage(12, 6));
        assertEquals(2, AuraListLayout.lastPage(13, 6));
        assertEquals(7, AuraListLayout.indexAt(120, 224, 100, 200, 220, 24, 6, 1, 12));
        assertEquals(12, AuraListLayout.indexAt(120, 203, 100, 200, 220, 24, 6, 2, 13));
        assertEquals(-1, AuraListLayout.indexAt(120, 224, 100, 200, 220, 24, 6, 2, 13));
    }
}
