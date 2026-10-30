package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneDamageChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class RuneDamageMathTest {
    @Test
    void everyStatusChannelUsesTheSameStackingContract() {
        for (RuneDamageChannel channel : RuneDamageChannel.values()) {
            RuneDamageMath.Snapshot snapshot = new RuneDamageMath.Snapshot();
            RuneDamageMath.ChannelStats stats = snapshot.channel(channel);
            stats.flat = 10.0D;
            stats.multiplier = 2.0D;
            stats.vulnerabilityPercent = 25.0D;
            snapshot.allDamageVulnerabilityPercent = 15.0D;
            snapshot.allDamageMultiplier = 1.5D;
            snapshot.temporaryAllDamageActive = true;
            snapshot.temporaryAllDamageMultiplier = 2.0D;

            // (5 + 10) * 2 * (1 + (25 + 15) / 100) * 1.5 * 2 = 126
            assertEquals(126.0D, RuneDamageMath.damage(5.0D, channel, snapshot), 0.000001D,
                    "channel " + channel + " must use shared stacking semantics");
        }
    }

    @Test
    void c1CompatibilityFacadeKeepsClickAndPoisonFormula() {
        RuneCombatMath.Snapshot legacy = new RuneCombatMath.Snapshot();
        legacy.clickFlat = 10.0D;
        legacy.clickMultiplier = 2.0D;
        legacy.poisonFlat = 8.0D;
        legacy.poisonMultiplier = 3.0D;
        legacy.clickVulnerabilityPercent = 10.0D;
        legacy.poisonVulnerabilityPercent = 20.0D;
        legacy.allDamageVulnerabilityPercent = 5.0D;
        legacy.allDamageMultiplier = 1.5D;
        legacy.rageActive = true;
        legacy.rageMultiplier = 2.0D;
        legacy.procDamageMultiplier = 4.0D;

        RuneDamageMath.Snapshot generic = RuneCombatMath.generic(legacy);
        double expectedClickWithoutProc = RuneDamageMath.damage(5.0D, RuneDamageChannel.CLICK, generic);
        double expectedPoison = RuneDamageMath.statusDamage(RuneDamageChannel.POISON, generic);

        assertEquals(expectedClickWithoutProc, RuneCombatMath.clickDamage(5.0D, legacy, false), 0.000001D);
        assertEquals(expectedClickWithoutProc * 4.0D, RuneCombatMath.clickDamage(5.0D, legacy, true), 0.000001D);
        assertEquals(expectedPoison, RuneCombatMath.poisonDamage(legacy), 0.000001D);
    }

    @Test
    void channelsRemainIndependent() {
        RuneDamageMath.Snapshot snapshot = new RuneDamageMath.Snapshot();
        snapshot.channel(RuneDamageChannel.BURNING).flat = 50.0D;
        snapshot.channel(RuneDamageChannel.CURSE).flat = 7.0D;

        assertEquals(50.0D, RuneDamageMath.statusDamage(RuneDamageChannel.BURNING, snapshot), 0.000001D);
        assertEquals(7.0D, RuneDamageMath.statusDamage(RuneDamageChannel.CURSE, snapshot), 0.000001D);
        assertEquals(0.0D, RuneDamageMath.statusDamage(RuneDamageChannel.DECAY, snapshot), 0.000001D);
    }
}
