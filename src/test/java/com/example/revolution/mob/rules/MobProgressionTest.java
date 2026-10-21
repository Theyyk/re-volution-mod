package com.example.revolution.mob.rules;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MobProgressionTest {
    public static void main(String[] args) {
        List<Integer> configured = MobProgressionRules.normalizeConfiguredSlots(
                Arrays.asList(9, 2, 5, 5, -1, 100, null));
        check(configured.equals(Arrays.asList(2, 5, 9)), "Configured slots must be unique, valid and sorted");

        Set<Integer> unlocked = new HashSet<>();
        check(MobProgressionRules.isUnlocked(2, configured, unlocked), "First configured stage must be open by default");
        check(!MobProgressionRules.isUnlocked(5, configured, unlocked), "Second stage must start locked");
        check(!MobProgressionRules.isUnlocked(9, configured, unlocked), "Later stage must start locked");
        check(!MobProgressionRules.isUnlocked(3, configured, unlocked), "Unconfigured slot must not be treated as a stage by pure rules");

        unlocked.add(5);
        check(MobProgressionRules.isUnlocked(5, configured, unlocked), "Explicit unlock must open the stage");
        check(!MobProgressionRules.isUnlocked(9, configured, unlocked), "Unlocking one stage must not skip the next stage");

        check(Integer.valueOf(5).equals(MobProgressionRules.nextConfiguredSlot(2, configured)), "2 must advance to 5");
        check(Integer.valueOf(9).equals(MobProgressionRules.nextConfiguredSlot(5, configured)), "5 must advance to 9");
        check(MobProgressionRules.nextConfiguredSlot(9, configured) == null, "Last stage must have no next stage");
        check(MobProgressionRules.nextConfiguredSlot(3, configured).equals(5), "Ordering must tolerate non-contiguous configured IDs");

        check(MobProgressionRules.previousConfiguredSlot(2, configured) == null, "First stage must have no prerequisite slot");
        check(Integer.valueOf(2).equals(MobProgressionRules.previousConfiguredSlot(5, configured)), "5 must require 2");
        check(Integer.valueOf(5).equals(MobProgressionRules.previousConfiguredSlot(9, configured)), "9 must require 5");
        check(!MobProgressionRules.isUnlocked(1, Collections.<Integer>emptyList(), Collections.<Integer>emptySet()),
                "Empty pure-rule chain must not invent a stage");

        System.out.println("v1.3.8 mob progression ordering/gating regression checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
