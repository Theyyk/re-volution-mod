package com.example.revolution.client.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.revolution.mob.appearance.MobAppearanceCatalog;
import com.example.revolution.mob.appearance.MobAppearanceDefinition;
import org.junit.jupiter.api.Test;

final class ForestMobPaletteCatalogTest {
    @Test
    void everyLocationOneAppearanceHasAnExplicitPalette() {
        assertEquals(41, ForestMobPaletteCatalog.size());
        for (MobAppearanceDefinition definition : MobAppearanceCatalog.location1()) {
            assertNotNull(ForestMobPaletteCatalog.get(definition.getAppearanceId()), definition.getAppearanceId());
        }
    }
}
