package com.example.customguimod;

import java.util.Arrays;

final class ClientRuneRanks {
    private static final int[] BEST_RANKS = new int[RuneInventory.BOOST_TYPE_COUNT];

    private ClientRuneRanks() {}

    static void accept(CardPacket packet) {
        if (packet == null) return;
        if (packet.getSlot() == -1) {
            Arrays.fill(BEST_RANKS, 0);
            return;
        }

        int rune = packet.getCardIndex();
        if (rune < 0 || rune >= BEST_RANKS.length) return;
        BEST_RANKS[rune] = Math.max(BEST_RANKS[rune], RuneProgression.clampRank(packet.getLayer()));
    }

    static int rankOf(int rune) {
        if (rune < 0 || rune >= BEST_RANKS.length) return 0;
        return BEST_RANKS[rune];
    }
}
