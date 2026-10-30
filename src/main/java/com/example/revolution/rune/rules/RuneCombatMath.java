package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneDamageChannel;

/**
 * Compatibility facade for the original C1 click + poison model.
 * Generic v2.3.0 channel math lives in RuneDamageMath.
 */
public final class RuneCombatMath {
    private RuneCombatMath() {}

    public static final class Snapshot {
        public Snapshot() {}
        public double clickFlat;
        public double clickMultiplier = 1.0D;
        public double poisonFlat;
        public double poisonMultiplier = 1.0D;
        public double clickVulnerabilityPercent;
        public double poisonVulnerabilityPercent;
        public double allDamageVulnerabilityPercent;
        public double allDamageMultiplier = 1.0D;
        public double procChancePercent;
        public double procDamageMultiplier = 1.0D;
        public boolean rageActive;
        public double rageMultiplier = 1.0D;
    }

    public static double clickDamage(double vanillaClickDamage, Snapshot snapshot, boolean procTriggered) {
        if (snapshot == null) return RuneDamageMath.sanitize(vanillaClickDamage);
        RuneDamageMath.Snapshot generic = generic(snapshot);
        double click = RuneDamageMath.damage(vanillaClickDamage, RuneDamageChannel.CLICK, generic);
        if (procTriggered) click *= RuneDamageMath.safeMultiplier(snapshot.procDamageMultiplier);
        return RuneDamageMath.sanitize(click);
    }

    public static double poisonDamage(Snapshot snapshot) {
        if (snapshot == null) return 0.0D;
        return RuneDamageMath.statusDamage(RuneDamageChannel.POISON, generic(snapshot));
    }

    public static double totalDamage(double vanillaClickDamage, Snapshot snapshot, boolean procTriggered) {
        return RuneDamageMath.sanitize(clickDamage(vanillaClickDamage, snapshot, procTriggered) + poisonDamage(snapshot));
    }

    public static RuneDamageMath.Snapshot generic(Snapshot snapshot) {
        RuneDamageMath.Snapshot generic = new RuneDamageMath.Snapshot();
        if (snapshot == null) return generic;

        RuneDamageMath.ChannelStats click = generic.channel(RuneDamageChannel.CLICK);
        click.flat = snapshot.clickFlat;
        click.multiplier = snapshot.clickMultiplier;
        click.vulnerabilityPercent = snapshot.clickVulnerabilityPercent;

        RuneDamageMath.ChannelStats poison = generic.channel(RuneDamageChannel.POISON);
        poison.flat = snapshot.poisonFlat;
        poison.multiplier = snapshot.poisonMultiplier;
        poison.vulnerabilityPercent = snapshot.poisonVulnerabilityPercent;

        generic.allDamageVulnerabilityPercent = snapshot.allDamageVulnerabilityPercent;
        generic.allDamageMultiplier = snapshot.allDamageMultiplier;
        generic.temporaryAllDamageActive = snapshot.rageActive;
        generic.temporaryAllDamageMultiplier = snapshot.rageMultiplier;
        return generic;
    }
}
