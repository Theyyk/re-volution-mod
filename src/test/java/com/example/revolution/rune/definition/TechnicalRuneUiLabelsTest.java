package com.example.revolution.rune.definition;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class TechnicalRuneUiLabelsTest {
    @Test void technicalSlotsRemainNeutralAndDeterministic() {
        assertEquals("Коллекция 02 / Руна 01", TechnicalRuneUiLabels.runeName(1, 0));
        assertEquals("Коллекция 07 / Руна 26", TechnicalRuneUiLabels.runeName(6, 25));
    }

    @Test void knownEffectsAreReadableAndUnknownDoNotInventBonus() {
        assertEquals("Отложенный дополнительный урон", TechnicalRuneUiLabels.effect("DELAYED_DAMAGE_BONUS"));
        assertEquals("Неизвестный технический эффект", TechnicalRuneUiLabels.effect("UNDEFINED_EFFECT"));
        assertEquals("горение", TechnicalRuneUiLabels.channel(RuneDamageChannel.BURNING));
    }
}
