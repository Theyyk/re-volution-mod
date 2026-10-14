package com.example.customguimod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compact v1.2.9 presentation layer for rune stats.
 * The GUI renders only final values now; RuneStat.Breakdown keeps the detailed
 * base/awakening/enhancement/other split ready for a later expanded tooltip.
 */
final class RuneStatDisplay {
    private RuneStatDisplay() {}

    static List<String> finalValueLines(RuneStat[] stats, int rank, double enhancementMultiplier) {
        if (stats == null || stats.length == 0) return Collections.emptyList();

        List<String> lines = new ArrayList<>();
        int limit = Math.min(3, stats.length);
        for (int i = 0; i < limit; i++) {
            RuneStat stat = stats[i];
            if (stat == null || stat.type == null) continue;
            RuneStat.Breakdown breakdown = stat.resolve(rank, enhancementMultiplier);
            lines.add(stat.type.displayName + " " + stat.type.format(breakdown.finalValue));
        }
        return lines;
    }
}
