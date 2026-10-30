package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneDamageChannel;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneDamageMath;
import com.example.revolution.rune.state.RuneCombatState;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class RuneGenericCombatChannelsTest {
    @Test
    void unlockedBurningCollectionContributesGenericModifiersWithoutPetXpLeak() {
        UUID player = UUID.randomUUID();
        PlayerRepository players = (PlayerRepository) Proxy.newProxyInstance(
                PlayerRepository.class.getClassLoader(),
                new Class<?>[]{PlayerRepository.class, RuneCollectionStateRepository.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getUnlockedCollections":
                            return EnumSet.of(RuneCollectionId.COLLECTION_01, RuneCollectionId.COLLECTION_06);
                        case "getRunesInCollection":
                            int collection = (int) args[1];
                            if (collection == RuneCollectionId.COLLECTION_06.legacyIndex()) {
                                return Arrays.asList(
                                        new RuneRecord(0, 0, 1, 1),
                                        new RuneRecord(18, 18, 3, 1),
                                        new RuneRecord(20, 20, 1, 1),
                                        new RuneRecord(22, 22, 1, 1),
                                        new RuneRecord(23, 23, 1, 1));
                            }
                            return Collections.emptyList();
                        default:
                            return defaultValue(method.getReturnType());
                    }
                });
        RuneEnhancementRepository enhancements = (RuneEnhancementRepository) Proxy.newProxyInstance(
                RuneEnhancementRepository.class.getClassLoader(),
                new Class<?>[]{RuneEnhancementRepository.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("getLevels")) return defaultValue(method.getReturnType());
                    int[] levels = new int[26];
                    if ((int) args[1] == RuneCollectionId.COLLECTION_06.legacyIndex()) levels[0] = 2;
                    return levels;
                });

        RuneCombatService combat = new RuneCombatService(players, enhancements, new RuneCombatState());
        RuneDamageMath.Snapshot snapshot = combat.resolveDamageStable(player);

        RuneDamageMath.ChannelStats burning = snapshot.existing(RuneDamageChannel.BURNING);
        assertEquals(12.06D, burning.flat, 0.000001D,
                "base 10 + enhanced delta 2 amplified by x1.03 enhancement effectiveness");
        assertEquals(1.04D, burning.multiplier, 0.000001D);
        assertEquals(1.03D, burning.effectivenessMultiplier, 0.000001D);
        assertEquals(12.918672D,
                RuneDamageMath.statusDamage(RuneDamageChannel.BURNING, snapshot), 0.000001D);

        RuneDamageMath.ChannelStats click = snapshot.existing(RuneDamageChannel.CLICK);
        assertEquals(1.02D, click.multiplier, 0.000001D);
        assertEquals(0.0D, click.flat, 0.000001D);
        assertEquals(1.0D, click.effectivenessMultiplier, 0.000001D);

        assertNull(snapshot.existing(RuneDamageChannel.DECAY));
        // Slot 18 is PET_XP_MULTIPLIER. Its presence must not alter any damage channel.
        assertEquals(2, snapshot.channels().size());
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
