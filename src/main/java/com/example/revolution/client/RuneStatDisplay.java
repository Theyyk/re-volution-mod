package com.example.revolution.client;

import com.example.revolution.rune.definition.RuneStat;
import com.example.revolution.rune.definition.RuneStatType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compact v1.2.9 presentation layer for rune stats.
 * The GUI renders only final values now; RuneStat.Breakdown keeps the detailed
 * base/awakening/enhancement/other split ready for a later expanded tooltip.
 */
public final class RuneStatDisplay {
    private RuneStatDisplay() {}

    public static List<String> finalValueLines(RuneStat[] stats, int rank, double enhancementMultiplier) {
        return finalValueLines(stats, rank, enhancementMultiplier, 0.0D);
    }

    public static List<String> finalValueLines(RuneStat[] stats,
                                               int rank,
                                               double enhancementMultiplier,
                                               double targetedBasePowerBonus) {
        if (stats == null || stats.length == 0) return Collections.emptyList();

        List<String> lines = new ArrayList<>();
        int limit = Math.min(3, stats.length);
        for (int i = 0; i < limit; i++) {
            RuneStat stat = stats[i];
            if (stat == null || stat.type == null) continue;
            boolean targetedFlat = stat.type == RuneStatType.CLICK_DAMAGE_FLAT
                    || stat.type == RuneStatType.POISON_DAMAGE_FLAT;
            RuneStat.Breakdown breakdown = stat.resolve(
                    rank,
                    enhancementMultiplier,
                    targetedFlat ? targetedBasePowerBonus : 0.0D
            );
            lines.add(stat.type.displayName + " " + stat.type.format(breakdown.finalValue));
        }
        return lines;
    }
}
