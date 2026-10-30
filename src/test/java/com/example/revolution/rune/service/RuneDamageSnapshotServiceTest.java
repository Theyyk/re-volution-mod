package com.example.revolution.rune.service;

import com.example.revolution.rune.definition.RuneDamageChannel;
import com.example.revolution.rune.definition.RuneStatType;
import com.example.revolution.rune.rules.RuneDamageMath;

public final class RuneDamageSnapshotServiceTest {
    public static void main(String[] args) {
        RuneDamageMath.Snapshot snapshot = new RuneDamageMath.Snapshot();

        RuneDamageSnapshotService.accumulate(snapshot, RuneStatType.BURNING_DAMAGE_FLAT, 10.0D);
        RuneDamageSnapshotService.accumulate(snapshot, RuneStatType.BURNING_DAMAGE_MULTIPLIER, 2.0D);
        RuneDamageSnapshotService.accumulate(snapshot, RuneStatType.BURNING_VULNERABILITY, 25.0D);
        RuneDamageSnapshotService.accumulate(snapshot, RuneStatType.ALL_DAMAGE_VULNERABILITY, 10.0D);
        RuneDamageSnapshotService.accumulate(snapshot, RuneStatType.ALL_DAMAGE_MULTIPLIER, 1.5D);

        check(close(RuneDamageMath.statusDamage(RuneDamageChannel.BURNING, snapshot), 40.5D),
                "burning stacking contract changed");

        RuneDamageSnapshotService.accumulate(snapshot, RuneStatType.BLEEDING_DAMAGE_FLAT, 4.0D);
        RuneDamageSnapshotService.accumulate(snapshot, RuneStatType.CURSE_DAMAGE_FLAT, 5.0D);
        RuneDamageSnapshotService.accumulate(snapshot, RuneStatType.WITHER_DAMAGE_FLAT, 6.0D);
        RuneDamageSnapshotService.accumulate(snapshot, RuneStatType.DECAY_DAMAGE_FLAT, 7.0D);

        check(RuneDamageMath.statusDamage(RuneDamageChannel.BLEEDING, snapshot) > 0.0D, "bleeding not accumulated");
        check(RuneDamageMath.statusDamage(RuneDamageChannel.CURSE, snapshot) > 0.0D, "curse not accumulated");
        check(RuneDamageMath.statusDamage(RuneDamageChannel.WITHER, snapshot) > 0.0D, "wither not accumulated");
        check(RuneDamageMath.statusDamage(RuneDamageChannel.DECAY, snapshot) > 0.0D, "decay not accumulated");

        System.out.println("RuneDamageSnapshotServiceTest passed");
    }

    private static boolean close(double left, double right) {
        return Math.abs(left - right) < 0.000001D;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
