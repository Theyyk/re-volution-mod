package com.example.revolution.rune;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneStatType;
import com.example.revolution.rune.rules.RuneCombatMath;
import com.example.revolution.rune.service.RuneEffectBalanceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Collection 01 Infection Mark must enhance POISON only.
 * ALL_DAMAGE_VULNERABILITY remains a separate, intentionally universal stat.
 */
public final class PoisonInfectionMarkTargetRegressionTest {
    @Test void infectionMarkTargetsOnlyPoisonAtEveryRank() {
        RuneEffectBalanceService balance =
                new RuneEffectBalanceService(BalanceBootstrap.activateBundledDefaults());
        for (int slot = 22; slot <= 24; slot++) {
            for (int rank = 1; rank <= 4; rank++) {
                List<RuneEffectBalanceService.ResolvedEffect> effects =
                        balance.effects(RuneCollectionId.COLLECTION_01, slot, rank, 0);
                assertEquals(1, effects.size(), "Infection Mark effect count");
                RuneEffectBalanceService.ResolvedEffect effect = effects.get(0);
                assertEquals(RuneEffectBalanceService.EffectType.STAT, effect.type);
                assertEquals(RuneStatType.POISON_VULNERABILITY, effect.statType);
                assertTrue(effect.primaryValue > 0.0D, "existing Balance Core potency preserved");
            }
        }
    }

    @Test void poisonVulnerabilityCannotAmplifyClickWhileUniversalStillCan() {
        RuneCombatMath.Snapshot snapshot = new RuneCombatMath.Snapshot();
        snapshot.poisonFlat = 100.0D;
        snapshot.poisonVulnerabilityPercent = 30.0D;
        assertEquals(10.0D, RuneCombatMath.clickDamage(10.0D, snapshot, false), 0.000001D);
        assertEquals(130.0D, RuneCombatMath.poisonDamage(snapshot), 0.000001D);
        snapshot.allDamageVulnerabilityPercent = 20.0D;
        assertEquals(12.0D, RuneCombatMath.clickDamage(10.0D, snapshot, false), 0.000001D);
        assertEquals(150.0D, RuneCombatMath.poisonDamage(snapshot), 0.000001D);
    }
}
