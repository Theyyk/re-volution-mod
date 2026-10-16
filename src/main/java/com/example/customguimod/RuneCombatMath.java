package com.example.customguimod;

/** Pure combat calculation for the v1.2.10 rune damage model. */
final class RuneCombatMath {
    private RuneCombatMath() {}

    static final class Snapshot {
        double clickFlat;
        double clickMultiplier = 1.0D;
        double poisonFlat;
        double poisonMultiplier = 1.0D;
        double clickVulnerabilityPercent;
        double poisonVulnerabilityPercent;
        double allDamageVulnerabilityPercent;
        double allDamageMultiplier = 1.0D;
        double procChancePercent;
        double procDamageMultiplier = 1.0D;
        boolean rageActive;
        double rageMultiplier = 1.0D;
    }

    static double totalDamage(double vanillaClickDamage, Snapshot snapshot, boolean procTriggered) {
        if (snapshot == null) return sanitize(vanillaClickDamage);

        double click = Math.max(0.0D, sanitize(vanillaClickDamage) + nonNegative(snapshot.clickFlat));
        click *= safeMultiplier(snapshot.clickMultiplier);
        click *= vulnerabilityMultiplier(snapshot.clickVulnerabilityPercent + snapshot.allDamageVulnerabilityPercent);
        if (procTriggered) click *= safeMultiplier(snapshot.procDamageMultiplier);

        double poison = nonNegative(snapshot.poisonFlat);
        poison *= safeMultiplier(snapshot.poisonMultiplier);
        poison *= vulnerabilityMultiplier(snapshot.poisonVulnerabilityPercent + snapshot.allDamageVulnerabilityPercent);

        double total = (click + poison) * safeMultiplier(snapshot.allDamageMultiplier);
        if (snapshot.rageActive) total *= safeMultiplier(snapshot.rageMultiplier);
        return sanitize(total);
    }

    private static double vulnerabilityMultiplier(double percent) {
        if (!Double.isFinite(percent)) return 1.0D;
        return Math.max(0.0D, 1.0D + percent / 100.0D);
    }

    private static double safeMultiplier(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 1.0D;
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value)) return Float.MAX_VALUE;
        return Math.max(0.0D, Math.min((double) Float.MAX_VALUE, value));
    }
}
