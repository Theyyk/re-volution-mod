package com.example.revolution.rune.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneDamageChannel;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneDamageMath;
import com.example.revolution.rune.state.RuneCombatState;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class RuneTemporaryDamageMultiplierTest {
    @Test
    void targetedTemporaryMultiplierOnlyAffectsItsChannelUntilExpiry() {
        BalanceService balance = new BalanceService(
                new BalanceConfigParser(), new BalanceConfigValidator(),
                new BalanceConfigCodec(), new BalanceCurveEngine());
        balance.activate("{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.slot.0.group\":\"temp_burning\","
                + "\"runes.poison.effect.temp_burning.count\":\"1\","
                + "\"runes.poison.effect.temp_burning.1.type\":\"TEMPORARY_DAMAGE_MULTIPLIER\","
                + "\"runes.poison.effect.temp_burning.1.tags\":\"burning,temp\","
                + "\"runes.poison.effect.temp_burning.1.target_selector\":\"BURNING\","
                + "\"runes.poison.effect.temp_burning.1.trigger\":\"HIT_MOB\","
                + "\"runes.poison.effect.temp_burning.1.max_stacks\":\"1\","
                + "\"runes.poison.effect.temp_burning.1.rank.1.primary\":\"1.5\","
                + "\"runes.poison.effect.temp_burning.1.rank.1.duration_seconds\":\"2\"},"
                + "\"curves\":{\"test\":{\"mode\":\"FIXED\",\"value\":\"1\"}}}");

        UUID player = UUID.randomUUID();
        PlayerRepository players = (PlayerRepository) Proxy.newProxyInstance(
                PlayerRepository.class.getClassLoader(),
                new Class<?>[]{PlayerRepository.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getCollectionNames": return Collections.singletonList("Primary");
                        case "getRunesInCollection": return Collections.singletonList(new RuneRecord(0, 0, 1, 1));
                        default: return defaultValue(method.getReturnType());
                    }
                });
        RuneEnhancementRepository enhancements = (RuneEnhancementRepository) Proxy.newProxyInstance(
                RuneEnhancementRepository.class.getClassLoader(),
                new Class<?>[]{RuneEnhancementRepository.class},
                (proxy, method, args) -> method.getName().equals("getLevels")
                        ? new int[26] : defaultValue(method.getReturnType()));

        RuneCombatState state = new RuneCombatState();
        RuneCombatService combat = new RuneCombatService(
                players, enhancements, state, new RuneEffectBalanceService(balance));

        combat.activateRageOnMobHit(player, 100L);
        RuneDamageMath.Snapshot active = combat.resolveDamage(player, 101L);
        assertEquals(1.5D, active.existing(RuneDamageChannel.BURNING).multiplier, 0.000001D);
        assertNull(active.existing(RuneDamageChannel.CURSE));
        assertNull(active.existing(RuneDamageChannel.CLICK));

        RuneDamageMath.Snapshot expired = combat.resolveDamage(player, 140L);
        assertNull(expired.existing(RuneDamageChannel.BURNING),
                "target channel is not materialized after temporary effect expires");
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0F;
        if (type == double.class) return 0.0D;
        if (type == char.class) return '\0';
        return null;
    }
}
