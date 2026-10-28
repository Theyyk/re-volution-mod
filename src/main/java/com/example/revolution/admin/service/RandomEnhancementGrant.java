package com.example.revolution.admin.service;

import java.util.function.IntUnaryOperator;

/** Plans every random roll before any write; uses the existing fixed-slot enhancement contract. */
public final class RandomEnhancementGrant {
    public static final int MAX_COUNT = 100000;
    private RandomEnhancementGrant() { }
    public static int[] plan(int[] levels, int count, IntUnaryOperator random) {
        if (levels == null || levels.length != 26 || count < 1 || count > MAX_COUNT || random == null)
            throw new IllegalArgumentException("Invalid enhancement grant");
        int[] increments = new int[levels.length];
        for (int i = 0; i < count; i++) {
            int slot = random.applyAsInt(levels.length);
            if (slot < 0 || slot >= levels.length || levels[slot] < 0 ||
                    (long) levels[slot] + increments[slot] + 1 > Integer.MAX_VALUE)
                throw new IllegalArgumentException("Enhancement slot limit reached");
            increments[slot]++;
        }
        return increments;
    }
}
