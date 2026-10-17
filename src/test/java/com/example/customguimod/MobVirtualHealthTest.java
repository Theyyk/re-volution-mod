package com.example.customguimod;

import java.math.BigDecimal;

final class MobVirtualHealthTest {
    static void run() {
        check(MobManager.nativeHealthFor(new BigDecimal("1")) == 1.0D,
                "Small mob HP should keep its native value");
        check(MobManager.nativeHealthFor(new BigDecimal("1024")) == 1024.0D,
                "Vanilla max health boundary changed");
        check(MobManager.nativeHealthFor(new BigDecimal("1000000000000000000000000000000000000000000")) == 1024.0D,
                "Arbitrary precision mob HP must still use the vanilla backing cap");

        BigDecimal huge = new BigDecimal("1000000000000000000000000000000000000000000");
        BigDecimal remaining = MobManager.remainingHealthAfterDamage(huge, 90.0D);
        check(remaining.equals(new BigDecimal("999999999999999999999999999999999999999910")),
                "Virtual HP must preserve values beyond long range exactly");
        check(MobManager.remainingHealthAfterDamage(new BigDecimal("100"), 150.0D).signum() == 0,
                "Virtual HP must clamp lethal damage at zero");
        check(MobManager.remainingHealthAfterDamage(new BigDecimal("100"), -5.0D).compareTo(new BigDecimal("100")) == 0,
                "Negative damage must not heal virtual HP");

        BigDecimal precise = LargeNumberValue.parseWithUnit("1.0912121", 2);
        check(precise.compareTo(new BigDecimal("1091212.1")) == 0,
                "Unit input must retain hidden precision for calculations");
        check("1.09M".equals(LargeNumberFormatter.format(precise)),
                "Display must hide decimals after hundredths without changing stored HP");

        MobManager.MobSpawn spawn = new MobManager.MobSpawn(
                "TestMobHP", "zombie", huge, "coins", 100, 0.0D, 64.0D, 0.0D);
        check(spawn.hp.compareTo(huge) == 0 && spawn.currentHp.compareTo(huge) == 0,
                "A new managed mob must keep arbitrary precision configured HP");

        System.out.println("v1.2.11 arbitrary precision managed mob health regression checks passed");
    }

    public static void main(String[] args) { run(); }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
