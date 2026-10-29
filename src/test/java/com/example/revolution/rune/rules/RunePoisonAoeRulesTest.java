package com.example.revolution.rune.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class RunePoisonAoeRulesTest {
    public static void main(String[] args) {
        run();
        System.out.println("Poison AoE DPS distribution passed");
    }

    @Test void zbtPoisonAoeCurveAndTickBudget() { run(); }

    private static void run() {
        double[] expected = {0.0D, 1.0D, 1.5D, 1.8D, 2.0D, 2.2D, 2.3D, 2.4D, 2.5D, 2.6D, 2.7D};
        for (int n = 1; n <= 10; n++) {
            assertEquals(expected[n], RunePoisonAoeRules.totalMultiplier(n), 0.0000001D);
            double tick = RunePoisonAoeRules.perTargetTick(100.0D, n);
            assertEquals(100.0D * expected[n] / n / 5.0D, tick, 0.0000001D);
            assertEquals(100.0D * expected[n], tick * n * 5.0D, 0.0000001D,
                    "five ticks must reproduce the total AoE DPS budget");
        }
        assertEquals(2.7D, RunePoisonAoeRules.totalMultiplier(11), 0.0000001D,
                "targets above the defined curve use the 10-target total multiplier cap");
        assertEquals(0.0D, RunePoisonAoeRules.perTargetTick(100.0D, 0), 0.0D);
        assertEquals(4, RunePoisonAoeRules.MC_TICK_INTERVAL);
        assertEquals(5, RunePoisonAoeRules.TICKS_PER_SECOND);
    }
}
