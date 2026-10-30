package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneDamageChannel;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.state.RuneCombatState;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class RuneTransientEffectRuntimeTest {
    @Test
    void resolvesDelayedEffectOnlyFromUnlockedFactualRuneState() {
        UUID player = UUID.randomUUID();
        PlayerRepository players = (PlayerRepository) Proxy.newProxyInstance(
                PlayerRepository.class.getClassLoader(),
                new Class<?>[]{PlayerRepository.class, RuneCollectionStateRepository.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getUnlockedCollections":
                            return EnumSet.of(RuneCollectionId.COLLECTION_01, RuneCollectionId.COLLECTION_06);
                        case "getRunesInCollection":
                            return (int) args[1] == RuneCollectionId.COLLECTION_06.legacyIndex()
                                    ? Collections.singletonList(new RuneRecord(19, 19, 2, 1))
                                    : Collections.emptyList();
                        default:
                            return defaultValue(method.getReturnType());
                    }
                });
        RuneEnhancementRepository enhancements = (RuneEnhancementRepository) Proxy.newProxyInstance(
                RuneEnhancementRepository.class.getClassLoader(),
                new Class<?>[]{RuneEnhancementRepository.class},
                (proxy, method, args) -> method.getName().equals("getLevels")
                        ? new int[26] : defaultValue(method.getReturnType()));
        RuneEffectBalanceService effects = new RuneEffectBalanceService(BalanceBootstrap.activateBundledDefaults());
        RuneCombatService combat = new RuneCombatService(players, enhancements, new RuneCombatState(), effects);
        RuneTransientEffectRuntime runtime = new RuneTransientEffectRuntime(players, enhancements, effects, combat);

        List<RuneTransientEffectRuntime.DelayedEffectDescriptor> resolved = runtime.resolveDelayedEffects(player);
        assertEquals(1, resolved.size());
        RuneTransientEffectRuntime.DelayedEffectDescriptor effect = resolved.get(0);
        assertEquals(RuneCollectionId.COLLECTION_06, effect.collectionId);
        assertEquals(19, effect.runeIndex);
        assertEquals(RuneDamageChannel.BURNING, effect.channel);
        assertEquals(1.08D, effect.multiplier, 0.000001D);
        assertEquals(40L, effect.delayTicks);
        assertEquals("HIT_MOB", effect.trigger);
        assertEquals("TARGET_ALIVE", effect.condition);
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
