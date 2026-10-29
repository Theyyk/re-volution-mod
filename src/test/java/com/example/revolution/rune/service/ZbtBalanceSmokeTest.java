package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneCombatMath;
import com.example.revolution.rune.rules.RuneProgression;
import com.example.revolution.rune.state.RuneCombatState;
import com.example.revolution.rune.state.RuneInventory;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Diagnostic smoke for the current Draft ZBT combat snapshot. No pacing value here is promoted to Final. */
public final class ZbtBalanceSmokeTest {
    private static final double BASE_CLICK = 1.0D;
    private static final double BOSS_HP = 30_000_000.0D;
    private static final int[] CPS_PROFILES = {5, 10, 15, 20};

    public static void main(String[] args) {
        runProfiles();
    }

    @Test
    void cpsFiveTenFifteenTwentyStayFiniteAndMonotonic() {
        runProfiles();
    }

    private static void runProfiles() {
        smokeRank("Ordinary", RuneProgression.NORMAL_RANK);
        smokeRank("Advanced", RuneProgression.ADVANCED_RANK);
    }

    private static void smokeRank(String label, int rank) {
        UUID player = UUID.randomUUID();
        List<RuneRecord> runes = fullCollection(rank);
        PlayerRepository players = proxy(PlayerRepository.class, (target, method, values) -> {
            switch (method.getName()) {
                case "getCollectionNames": return Collections.singletonList("Poison");
                case "getRunesInCollection": return runes;
                case "getActiveCollection": return 0;
                default: throw new AssertionError(method);
            }
        });
        RuneEnhancementRepository enhancements = proxy(RuneEnhancementRepository.class, (target, method, values) -> {
            if (method.getName().equals("getLevels")) return new int[RuneInventory.BOOST_TYPE_COUNT];
            throw new AssertionError(method);
        });

        RuneCombatService combat = new RuneCombatService(players, enhancements, new RuneCombatState());
        RuneCombatMath.Snapshot snapshot = combat.resolveStable(player);
        assertTrue(snapshot != null, "full collection must resolve combat snapshot");
        snapshot.rageActive = true; // after the first hit; normal manual CPS keeps the buff refreshed.

        double noProcClick = RuneCombatMath.clickDamage(BASE_CLICK, snapshot, false);
        double procClick = RuneCombatMath.clickDamage(BASE_CLICK, snapshot, true);
        double procChance = Math.max(0.0D, Math.min(100.0D, snapshot.procChancePercent)) / 100.0D;
        double expectedClick = noProcClick * (1.0D - procChance) + procClick * procChance;
        double poisonDps = RuneCombatMath.poisonDamage(snapshot);

        assertFinitePositive(noProcClick, label + " no-proc click");
        assertFinitePositive(expectedClick, label + " expected click");
        assertFinitePositive(poisonDps, label + " poison DPS");

        double previous = 0.0D;
        for (int cps : CPS_PROFILES) {
            double totalDps = expectedClick * cps + poisonDps;
            double bossTtkSeconds = BOSS_HP / totalDps;
            assertFinitePositive(totalDps, label + " total DPS at " + cps + " CPS");
            assertFinitePositive(bossTtkSeconds, label + " boss TTK at " + cps + " CPS");
            assertTrue(totalDps > previous, "DPS must increase with manual CPS");
            previous = totalDps;
            System.out.println("ZBT smoke " + label + " " + cps + " CPS: clickExpected="
                    + expectedClick + ", poisonDps=" + poisonDps + ", totalDps=" + totalDps
                    + ", stage41TtkSeconds=" + bossTtkSeconds);
        }
    }

    private static List<RuneRecord> fullCollection(int rank) {
        List<RuneRecord> runes = new ArrayList<>();
        for (int slot = 0; slot < RuneInventory.BOOST_TYPE_COUNT; slot++) {
            runes.add(new RuneRecord(slot, slot, rank, 1));
        }
        return runes;
    }

    private static void assertFinitePositive(double value, String label) {
        assertTrue(Double.isFinite(value) && value > 0.0D, label + " must be finite and positive: " + value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
