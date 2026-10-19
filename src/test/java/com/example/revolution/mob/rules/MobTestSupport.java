package com.example.revolution.mob.rules;

/** Test-only bridge; radius policy is not a public production API. */
public final class MobTestSupport {
    private MobTestSupport() {}
    public static boolean isWithinStatusRadiusSq(double distance) { return MobHealthRules.isWithinStatusRadiusSq(distance); }
}
