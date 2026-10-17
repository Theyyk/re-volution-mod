package com.example.customguimod;

final class RuneCombatMathTest {
    static void run() {
        RuneStat targeted = RuneStat.scaled(RuneStatType.CLICK_DAMAGE_FLAT, 4.0D);
        RuneStat.Breakdown targetedResolved = targeted.resolve(
                RuneInventory.IMPROVED_RANK,
                2.0D,
                2.0D
        );
        check(close(targetedResolved.baseValue, 6.0D),
                "Targeted power must be added to the starting value before rank scaling");
        check(close(targetedResolved.finalValue, 36.0D),
                "Targeted power order should be (base + targeted) -> rank -> enhancement");

        RuneCombatMath.Snapshot snapshot = new RuneCombatMath.Snapshot();
        snapshot.clickFlat = 10.0D;
        snapshot.clickMultiplier = 2.0D;
        snapshot.poisonFlat = 5.0D;
        snapshot.poisonMultiplier = 3.0D;
        snapshot.poisonVulnerabilityPercent = 20.0D;
        snapshot.allDamageMultiplier = 1.5D;
        snapshot.procDamageMultiplier = 5.0D;

        double normal = RuneCombatMath.totalDamage(2.0D, snapshot, false);
        check(close(normal, 63.0D),
                "Click and poison channels should combine after their own multipliers and vulnerability");

        double proc = RuneCombatMath.totalDamage(2.0D, snapshot, true);
        check(close(proc, 207.0D),
                "Click proc should multiply the click channel without multiplying poison a second time");

        snapshot.rageActive = true;
        snapshot.rageMultiplier = 2.0D;
        double rage = RuneCombatMath.totalDamage(2.0D, snapshot, false);
        check(close(rage, 126.0D),
                "Active rage should multiply all combined damage channels");

        RuneCombatMath.Snapshot vulnerability = new RuneCombatMath.Snapshot();
        vulnerability.clickFlat = 8.0D;
        vulnerability.poisonFlat = 10.0D;
        vulnerability.poisonVulnerabilityPercent = 25.0D;
        double vulnerable = RuneCombatMath.totalDamage(2.0D, vulnerability, false);
        check(close(vulnerable, 22.5D),
                "Poison vulnerability must affect poison damage only");

        RuneCombatMath.Snapshot neutral = new RuneCombatMath.Snapshot();
        neutral.procDamageMultiplier = 1.0D;
        neutral.rageActive = true;
        neutral.rageMultiplier = 1.0D;
        check(close(RuneCombatMath.totalDamage(7.0D, neutral, true), 7.0D),
                "Neutral x1 multipliers must not change combat damage");

        check(MobManager.isWithinStatusRadiusSq(0.0D),
                "Status effects should include mobs at the player's position");
        check(MobManager.isWithinStatusRadiusSq(4.0D),
                "Status effects should include mobs exactly two blocks away");
        check(!MobManager.isWithinStatusRadiusSq(4.000001D),
                "Status effects must not hit mobs outside the two-block radius");
        check(!MobManager.isWithinStatusRadiusSq(Double.NaN),
                "Invalid distances must not be accepted as status targets");

        System.out.println("v1.2.10 rune combat math and status radius regression checks passed");
    }

    public static void main(String[] args) {
        run();
    }

    private static boolean close(double left, double right) {
        return Math.abs(left - right) < 0.000001D;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
