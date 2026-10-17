package com.example.customguimod;

final class MobVirtualHealthTest {
    static void run() {
        check(MobManager.nativeHealthFor(1L) == 1.0D, "Small mob HP should keep its native value");
        check(MobManager.nativeHealthFor(1024L) == 1024.0D, "Vanilla max health boundary changed");
        check(MobManager.nativeHealthFor(100000000L) == 1024.0D,
                "Configured mob HP above vanilla limit must use a 1024 native backing value");
        check(MobManager.nativeHealthFor(10_000_000_000L) == 1024.0D,
                "Managed mob HP above the int range must still use the vanilla backing cap");

        check(MobManager.remainingHealthAfterDamage(100000000.0D, 90.0D) == 99999910.0D,
                "Virtual HP must preserve large configured health values");
        check(MobManager.remainingHealthAfterDamage(10_000_000_000.0D, 90.0D) == 9_999_999_910.0D,
                "Virtual HP must support values beyond the 32-bit integer range");
        check(MobManager.remainingHealthAfterDamage(100.0D, 150.0D) == 0.0D,
                "Virtual HP must clamp lethal damage at zero");
        check(MobManager.remainingHealthAfterDamage(100.0D, -5.0D) == 100.0D,
                "Negative damage must not heal virtual HP");

        MobManager.MobSpawn spawn = new MobManager.MobSpawn(
                "TestMobHP", "zombie", 10_000_000_000L, "coins", 100, 0.0D, 64.0D, 0.0D);
        check(spawn.hp == 10_000_000_000L,
                "Configured managed mob HP must no longer be limited to int range");
        check(spawn.currentHp == 10_000_000_000.0D,
                "A new managed mob must start with its full configured virtual HP");

        System.out.println("v1.2.11 managed mob virtual health regression checks passed");
    }

    public static void main(String[] args) {
        run();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
