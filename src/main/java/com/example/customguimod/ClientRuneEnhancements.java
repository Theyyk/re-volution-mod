package com.example.customguimod;

import java.util.Arrays;

final class ClientRuneEnhancements {
    private static final int[] LEVELS = new int[RuneInventory.BOOST_TYPE_COUNT];

    private ClientRuneEnhancements() {}

    static void accept(RuneEnhancementPacket packet) {
        Arrays.fill(LEVELS, 0);
        if (packet == null) return;
        int[] incoming = packet.getLevels();
        int limit = Math.min(LEVELS.length, incoming.length);
        for (int i = 0; i < limit; i++) LEVELS[i] = Math.max(0, incoming[i]);
    }

    static int levelOf(int runeIndex) {
        if (runeIndex < 0 || runeIndex >= LEVELS.length) return 0;
        return LEVELS[runeIndex];
    }
}
