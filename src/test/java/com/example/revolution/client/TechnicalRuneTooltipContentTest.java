package com.example.revolution.client;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TechnicalRuneTooltipContentTest {
    @Test
    void exposesAllFixedSlotsAndMythicGenericEffects() {
        assertEquals("Collection 06 / Rune 26", TechnicalRuneTooltipContent.name(5, 25));

        List<String> delayed = TechnicalRuneTooltipContent.lines(5, 19, 4, 0);
        assertTrue(delayed.stream().anyMatch(line -> line.contains("Мифическая")));
        assertTrue(delayed.stream().anyMatch(line -> line.contains("DELAYED_DAMAGE_BONUS BURNING")));

        List<String> effectiveness = TechnicalRuneTooltipContent.lines(2, 20, 3, 0);
        assertTrue(effectiveness.stream().anyMatch(line -> line.contains("EFFECTIVENESS DECAY")));
    }

    @Test
    void petXpRemainsVisibleAsExplicitRuntimeNoOp() {
        List<String> pet = TechnicalRuneTooltipContent.lines(5, 18, 3, 0);
        assertTrue(pet.stream().anyMatch(line -> line.contains("PET_XP_MULTIPLIER")));
        assertTrue(pet.stream().anyMatch(line -> line.contains("runtime no-op")));
    }
}
