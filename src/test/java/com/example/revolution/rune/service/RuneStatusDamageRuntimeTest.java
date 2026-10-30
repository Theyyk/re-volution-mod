package com.example.revolution.rune.service;

import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.rune.definition.RuneDamageChannel;

public final class RuneStatusDamageRuntimeTest {
    public static void main(String[] args) {
        checkMapping(RuneDamageChannel.CLICK, GameplayDamageSource.CLICK, false);
        checkMapping(RuneDamageChannel.POISON, GameplayDamageSource.POISON, true);
        checkMapping(RuneDamageChannel.BURNING, GameplayDamageSource.BURNING, true);
        checkMapping(RuneDamageChannel.BLEEDING, GameplayDamageSource.BLEEDING, true);
        checkMapping(RuneDamageChannel.CURSE, GameplayDamageSource.CURSE, true);
        checkMapping(RuneDamageChannel.WITHER, GameplayDamageSource.WITHER, true);
        checkMapping(RuneDamageChannel.DECAY, GameplayDamageSource.DECAY, true);
        check(RuneStatusDamageRuntime.gameplaySource(null) == null, "null channel must stay neutral");
        check(!RuneStatusDamageRuntime.isStatusChannel(null), "null must not be a status channel");
        System.out.println("RuneStatusDamageRuntimeTest passed");
    }

    private static void checkMapping(
            RuneDamageChannel channel,
            GameplayDamageSource expected,
            boolean status) {
        check(RuneStatusDamageRuntime.gameplaySource(channel) == expected,
                "gameplay source changed for " + channel);
        check(RuneStatusDamageRuntime.isStatusChannel(channel) == status,
                "status classification changed for " + channel);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
