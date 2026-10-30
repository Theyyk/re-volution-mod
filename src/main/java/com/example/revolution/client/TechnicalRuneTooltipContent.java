package com.example.revolution.client;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneStatType;
import com.example.revolution.rune.rules.RuneProgression;
import com.example.revolution.rune.service.RuneEffectBalanceService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Functional v2.3 tooltip skeleton for fixed technical collections 2-7. */
final class TechnicalRuneTooltipContent {
    private static final RuneEffectBalanceService EFFECTS =
            new RuneEffectBalanceService(BalanceBootstrap.activateBundledDefaults());

    private TechnicalRuneTooltipContent() {}

    static String name(int collectionIndex, int runeIndex) {
        return String.format(Locale.ROOT, "Collection %02d / Rune %02d", collectionIndex + 1, runeIndex + 1);
    }

    static String type(int collectionIndex, int runeIndex) {
        RuneCollectionId collectionId = RuneCollectionId.fromLegacyIndex(collectionIndex);
        if (collectionId == null || runeIndex < 0 || runeIndex >= 26) return "UNKNOWN";
        List<RuneEffectBalanceService.ResolvedEffect> effects = EFFECTS.effects(collectionId, runeIndex, 1, 0);
        if (effects.isEmpty()) return "NONE";
        RuneEffectBalanceService.ResolvedEffect first = effects.get(0);
        if (first.statType != null) return first.statType.name();
        return first.type.name();
    }

    static List<String> lines(int collectionIndex, int runeIndex, int rank, int enhancementLevel) {
        RuneCollectionId collectionId = RuneCollectionId.fromLegacyIndex(collectionIndex);
        if (collectionId == null || collectionId == RuneCollectionId.COLLECTION_01
                || runeIndex < 0 || runeIndex >= 26 || rank <= 0) {
            return Collections.emptyList();
        }

        List<String> lines = new ArrayList<>();
        lines.add("§b" + name(collectionIndex, runeIndex));
        lines.add("§7Ранг: §f" + RuneProgression.rankName(rank));
        lines.add("§7Усиление: §f+" + Math.max(0, enhancementLevel));
        lines.add("§7Эффекты:");

        for (RuneEffectBalanceService.ResolvedEffect effect : EFFECTS.effects(
                collectionId, runeIndex, rank, enhancementLevel)) {
            lines.add("§f" + effectLine(effect));
        }
        return lines;
    }

    private static String effectLine(RuneEffectBalanceService.ResolvedEffect effect) {
        switch (effect.type) {
            case STAT:
                if (effect.statType == RuneStatType.PET_XP_MULTIPLIER) {
                    return "PET_XP_MULTIPLIER: x" + number(effect.primaryValue) + " (runtime no-op)";
                }
                return (effect.statType == null ? "STAT" : effect.statType.name())
                        + ": " + number(effect.primaryValue);
            case TARGETED_POWER:
                return "TARGETED_POWER -> " + effect.targetSelector + ": +" + number(effect.primaryValue);
            case CLICK_DAMAGE_PROC:
                return "PROC " + effect.targetSelector + ": " + number(effect.primaryValue)
                        + "% / x" + number(effect.secondaryValue);
            case TEMPORARY_ALL_DAMAGE_MULTIPLIER:
                return "TEMP_ALL_DAMAGE: x" + number(effect.primaryValue)
                        + " / " + number(effect.durationSeconds) + "s";
            case EFFECTIVENESS:
                return "EFFECTIVENESS " + effect.targetSelector + ": x" + number(effect.primaryValue);
            case DELAYED_DAMAGE_BONUS:
                return "DELAYED " + effect.targetSelector + ": x" + number(effect.primaryValue)
                        + " after " + number(effect.delaySeconds) + "s";
            case TEMPORARY_DAMAGE_MULTIPLIER:
                return "TEMP " + effect.targetSelector + ": x" + number(effect.primaryValue)
                        + " / " + number(effect.durationSeconds) + "s";
            case TEMPORARY_EFFECT_DURATION:
                return "DURATION " + effect.targetSelector + ": x" + number(effect.primaryValue);
            case ENHANCEMENT_EFFECTIVENESS:
                return "ENHANCEMENT_EFF " + effect.targetSelector + ": x" + number(effect.primaryValue);
            default:
                return effect.type.name() + ": " + number(effect.primaryValue);
        }
    }

    private static String number(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001D) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.3f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }
}
