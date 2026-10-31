package com.example.revolution.network;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GodWorshipRequestLimiterTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000029");

    @AfterEach void cleanup() { GodWorshipRequestLimiter.reset(); }

    @Test
    void mutationNonceMustIncreaseStrictly() {
        assertTrue(GodWorshipRequestLimiter.acceptMutation(PLAYER, 10L));
        assertFalse(GodWorshipRequestLimiter.acceptMutation(PLAYER, 10L));
        assertFalse(GodWorshipRequestLimiter.acceptMutation(PLAYER, 9L));
        assertTrue(GodWorshipRequestLimiter.acceptMutation(PLAYER, 11L));
    }

    @Test
    void clickWindowRejectsBurstAboveBalanceLimitAndResetsNextSecond() {
        long start = 10_000L;
        assertTrue(GodWorshipRequestLimiter.acceptClick(PLAYER, start, 2));
        assertTrue(GodWorshipRequestLimiter.acceptClick(PLAYER, start + 10L, 2));
        assertFalse(GodWorshipRequestLimiter.acceptClick(PLAYER, start + 20L, 2));
        assertTrue(GodWorshipRequestLimiter.acceptClick(PLAYER, start + 1_000L, 2));
    }
}
