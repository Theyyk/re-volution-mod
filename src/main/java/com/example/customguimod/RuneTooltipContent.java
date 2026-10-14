package com.example.customguimod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the compact rune tooltip content independently from GUI rendering.
 *
 * v1.2.10 keeps the established order:
 * Name -> Rank -> Type -> Enhancement -> final Effects.
 * Calculation details stay in RuneStat.Breakdown; the player-facing tooltip
 * continues to show only final values until a detailed view is explicitly added.
 */
final class RuneTooltipContent {
    private RuneTooltipContent() {}

    static List<String> lines(EarthRuneCatalog.Definition definition,
                              int rank,
                              int enhancementLevel,
                              double enhancementMultiplier) {
        if (definition == null) return Collections.emptyList();

        List<String> lines = new ArrayList<>();
        lines.add("§b" + definition.name);
        lines.add("§7Ранг: §f" + RuneProgression.rankName(rank));
        lines.add("§7Тип: §f" + definition.type);
        lines.add("§7Улучшение: §f+" + Math.max(0, enhancementLevel));

        List<String> stats = RuneStatDisplay.finalValueLines(
                definition.stats,
                rank,
                enhancementMultiplier
        );
        if (stats.size() == 1) {
            lines.add("§7Эффекты: §f" + stats.get(0));
        } else if (!stats.isEmpty()) {
            lines.add("§7Эффекты:");
            for (String stat : stats) {
                lines.add("§f" + stat);
            }
        }

        return lines;
    }
}
