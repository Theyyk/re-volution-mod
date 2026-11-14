package com.example.revolution.rune.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneCombatMath;
import com.example.revolution.rune.state.RuneCombatState;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Reproduces the E snapshot regression: hit-triggered buffs must be visible while active. */
public final class RuneLiveCombatProjectionTest {
    @Test
    void liveDeterministicSnapshotIncludesActiveBuffAndExpiresWithoutProc() {
        BalanceService balance = new BalanceService(
                new BalanceConfigParser(), new BalanceConfigValidator(),
                new BalanceConfigCodec(), new BalanceCurveEngine());
        balance.activate("{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.slot.0.group\":\"temp_all\","
                + "\"runes.poison.effect.temp_all.count\":\"1\","
                + "\"runes.poison.effect.temp_all.1.type\":\"TEMPORARY_ALL_DAMAGE_MULTIPLIER\","
                + "\"runes.poison.effect.temp_all.1.tags\":\"all,temp\","
                + "\"runes.poison.effect.temp_all.1.trigger\":\"HIT_MOB\","
                + "\"runes.poison.effect.temp_all.1.max_stacks\":\"1\","
                + "\"runes.poison.effect.temp_all.1.rank.1.primary\":\"1.5\","
                + "\"runes.poison.effect.temp_all.1.rank.1.duration_seconds\":\"2\"},"
                + "\"curves\":{\"test\":{\"mode\":\"FIXED\",\"value\":\"1\"}}}");
        UUID id = UUID.randomUUID();
        PlayerRepository players = (PlayerRepository) Proxy.newProxyInstance(
                PlayerRepository.class.getClassLoader(), new Class<?>[]{PlayerRepository.class},
                (proxy, method, args) -> {
                    if ("getCollectionNames".equals(method.getName())) return Collections.singletonList("Primary");
                    if ("getRunesInCollection".equals(method.getName()))
                        return Collections.singletonList(new RuneRecord(0, 0, 1, 1));
                    return defaultValue(method.getReturnType());
                });
        RuneEnhancementRepository enhancements = (RuneEnhancementRepository) Proxy.newProxyInstance(
                RuneEnhancementRepository.class.getClassLoader(), new Class<?>[]{RuneEnhancementRepository.class},
                (proxy, method, args) -> "getLevels".equals(method.getName())
                        ? new int[26] : defaultValue(method.getReturnType()));
        RuneCombatService combat = new RuneCombatService(
                players, enhancements, new RuneCombatState(), new RuneEffectBalanceService(balance));

        double before = RuneCombatMath.clickDamage(5.0D, combat.resolve(id, 100L), false);
        combat.activateRageOnMobHit(id, 100L);
        RuneCombatMath.Snapshot active = combat.resolve(id, 101L);
        assertTrue(active.rageActive);
        assertEquals(before * 1.5D, RuneCombatMath.clickDamage(5.0D, active, false), 0.000001D);
        assertFalse(combat.resolve(id, 140L).rageActive);
        assertEquals(before, RuneCombatMath.clickDamage(5.0D, combat.resolve(id, 140L), false), 0.000001D);
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
