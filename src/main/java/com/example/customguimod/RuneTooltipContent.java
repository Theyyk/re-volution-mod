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
        return lines(definition, rank, enhancementLevel, enhancementMultiplier, 0.0D);
    }

    static List<String> lines(EarthRuneCatalog.Definition definition,
                              int rank,
                              int enhancementLevel,
                              double enhancementMultiplier,
                              double targetedBasePowerBonus) {
        if (definition == null) return Collections.emptyList();

        List<String> lines = new ArrayList<>();
        lines.add("§b" + definition.name);
        lines.add("§7Ранг: §f" + RuneProgression.rankName(rank));
        lines.add("§7Тип: §f" + definition.type);
        lines.add("§7Улучшение: §f+" + Math.max(0, enhancementLevel));

        List<String> effects = new ArrayList<>();
        effects.addAll(RuneStatDisplay.finalValueLines(
                definition.stats,
                rank,
                enhancementMultiplier,
                targetedBasePowerBonus
        ));

        for (RuneTargetedPower targetedPower : definition.targetedPowers) {
            if (targetedPower != null) {
                effects.add(targetedPower.tooltipLine(rank, enhancementMultiplier));
            }
        }

        for (RuneCompositeEffect compositeEffect : definition.compositeEffects) {
            if (compositeEffect != null) {
                effects.addAll(compositeEffect.tooltipLines(rank, enhancementMultiplier));
            }
        }

        if (!effects.isEmpty()) {
            lines.add("§7Эффекты:");
            for (String effect : effects) {
                lines.add("§f" + effect);
            }
        }

        return lines;
    }
}
