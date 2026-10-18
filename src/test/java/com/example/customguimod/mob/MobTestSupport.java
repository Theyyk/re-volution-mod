package com.example.customguimod.mob;

/** Test-only bridge; radius policy is not a public production API. */
public final class MobTestSupport {
    private MobTestSupport() {}
    public static boolean isWithinStatusRadiusSq(double distance) { return MobManager.isWithinStatusRadiusSq(distance); }
}
