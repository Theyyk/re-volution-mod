package com.example.revolution.network;

import com.example.revolution.unique.UniqueRuneModel;
import com.example.revolution.unique.UniqueRuneService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UniqueRuneUiTextTest {
    @Test void russianIdentifiersAndRanksPreserveServerKeys() {
        assertEquals("Кристальная руна №1", UniqueRuneUiText.runeName("crystal_01", "Crystal Rune 01"));
        assertEquals("Ежедневная руна №8", UniqueRuneUiText.runeName("daily_08", "Daily Rune 08"));
        assertEquals("Руна подношений №8", UniqueRuneUiText.runeName("offerings_08", "Offerings Rune 08"));
        assertEquals("Мифическая", UniqueRuneUiText.rarity(UniqueRuneModel.Rarity.MYTHIC));
        assertEquals("Кристальные руны", UniqueRuneUiText.collection(UniqueRuneModel.CollectionId.CRYSTAL_COLLECTION));
    }

    @Test void effectsUseRealValuesAndUnknownsNeverInventBonuses() {
        Map<String, String> effects = new LinkedHashMap<>();
        effects.put("POISON_DAMAGE_MULTIPLIER", "0.125");
        effects.put("FUTURE_INTERNAL_EFFECT", "3");
        String result = UniqueRuneUiText.effects(effects);
        assertTrue(result.contains("Урон яда +0.125"));
        assertTrue(result.contains("Неизвестный технический эффект +3"));
        assertFalse(result.contains("FUTURE_INTERNAL_EFFECT"));
        assertEquals("Нет", UniqueRuneUiText.effects(null));
    }

    @Test void replayAndUnsupportedActionsDoNotNotify() {
        assertNull(UniqueRuneUiText.notification("daily", null, "Руна", 0));
        assertNull(UniqueRuneUiText.notification("purchase_crystal", null, "Руна", 0));
    }
}
