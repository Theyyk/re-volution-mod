package com.example.revolution.prestige;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayDamageSource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regression for Mastery/Pets effect channel selection in the real damage event layer. */
public final class PrestigeDamageSourceIntegrationTest {
    @Test
    void explicitChannelsDoNotInheritClickEffects() {
        UUID player = UUID.randomUUID();
        EffectResolver effects = new EffectResolver();
        effects.register((id, stat) -> {
            if (!player.equals(id)) return Collections.emptyList();
            switch (stat) {
                case ALL_DAMAGE_MULTIPLIER:
                    return Collections.singletonList(new EffectResolver.Contribution(stat, new BigDecimal("2"), "mastery:all"));
                case CLICK_DAMAGE_MULTIPLIER:
                    return Collections.singletonList(new EffectResolver.Contribution(stat, new BigDecimal("4"), "mastery:click"));
                case POISON_DAMAGE_MULTIPLIER:
                    return Collections.singletonList(new EffectResolver.Contribution(stat, new BigDecimal("6"), "mastery:poison"));
                case BURNING_DAMAGE_MULTIPLIER:
                    return Collections.singletonList(new EffectResolver.Contribution(stat, new BigDecimal("8"), "rune:burning"));
                default:
                    return Collections.emptyList();
            }
        });
        assertFactor("15", effects, player, GameplayDamageSource.CLICK, false);
        assertFactor("21", effects, player, GameplayDamageSource.POISON, false);
        assertFactor("27", effects, player, GameplayDamageSource.BURNING, false);
        assertFactor("3", effects, player, GameplayDamageSource.FIRE, false);
        assertFactor("3", effects, player, GameplayDamageSource.OTHER, false);
        assertFactor("21", effects, player, GameplayDamageSource.POISON, true);
        // Explicit context overrides the legacy poison indicator.
        assertFactor("15", effects, player, GameplayDamageSource.CLICK, true);
        assertFactor("15", effects, player, null, false);
        assertFactor("21", effects, player, null, true);
        assertEquals(0, BigDecimal.ONE.compareTo(PrestigeDamageEffectHandler.damageMultiplier(
                effects, UUID.randomUUID(), GameplayDamageSource.POISON, false)));
    }

    private static void assertFactor(String expected, EffectResolver effects, UUID player,
                                     GameplayDamageSource source, boolean legacyPoison) {
        assertEquals(0, new BigDecimal(expected).compareTo(
                PrestigeDamageEffectHandler.damageMultiplier(effects, player, source, legacyPoison)));
    }
}
