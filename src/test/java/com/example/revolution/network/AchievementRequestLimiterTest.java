package com.example.revolution.network;

import java.util.UUID;

/** Standalone regression for v2.10.0 Achievement action replay protection. */
public final class AchievementRequestLimiterTest {
    public static void main(String[] args) {
        AchievementRequestLimiter.reset();
        UUID player = UUID.randomUUID();
        check(AchievementRequestLimiter.acceptAction(player, 10L), "first nonce is accepted");
        check(!AchievementRequestLimiter.acceptAction(player, 10L), "duplicate nonce is rejected");
        check(!AchievementRequestLimiter.acceptAction(player, 9L), "lower nonce is rejected");
        check(AchievementRequestLimiter.acceptAction(player, 11L), "higher nonce is accepted");
        AchievementRequestLimiter.forget(player);
        check(AchievementRequestLimiter.acceptAction(player, 1L), "logout forget allows fresh session nonce");
        AchievementRequestLimiter.reset();
        check(AchievementRequestLimiter.acceptAction(player, 1L), "server reset clears replay state");
        AchievementRequestLimiter.reset();
        System.out.println("Achievement request replay limiter regression passed");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
