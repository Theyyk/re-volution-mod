package com.example.revolution.client;

import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneTechnicalCatalog;
import com.example.revolution.rune.definition.TechnicalRuneUiLabels;
import com.example.revolution.rune.rules.RuneProgression;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Functional v2.3 tooltip skeleton for fixed technical collections 2-7. */
final class TechnicalRuneTooltipContent {
    private TechnicalRuneTooltipContent() {}

    static String name(int collectionIndex, int runeIndex) {
        return TechnicalRuneUiLabels.runeName(collectionIndex, runeIndex);
    }

    static String type(int collectionIndex, int runeIndex) {
        RuneCollectionId collectionId = RuneCollectionId.fromLegacyIndex(collectionIndex);
        List<RuneTechnicalCatalog.EffectLabel> effects = RuneTechnicalCatalog.effects(collectionId, runeIndex);
        if (effects.isEmpty()) return "Нет эффекта";
        RuneTechnicalCatalog.EffectLabel first = effects.get(0);
        return TechnicalRuneUiLabels.effect(first.type) + (first.channel == null ? "" : " — " + TechnicalRuneUiLabels.channel(first.channel));
    }

    static List<String> lines(int collectionIndex, int runeIndex, int rank, int enhancementLevel) {
        RuneCollectionId collectionId = RuneCollectionId.fromLegacyIndex(collectionIndex);
        if (collectionId == null || collectionId == RuneCollectionId.COLLECTION_01
                || runeIndex < 0 || runeIndex >= 26 || rank <= 0) {
            return Collections.emptyList();
        }

        List<RuneTechnicalCatalog.EffectLabel> effects = RuneTechnicalCatalog.effects(collectionId, runeIndex);
        if (effects.isEmpty()) return Collections.emptyList();

        List<String> lines = new ArrayList<>();
        lines.add("§b" + name(collectionIndex, runeIndex));
        lines.add("§7Ранг: §f" + RuneProgression.rankName(rank));
        lines.add("§7Усиление: §f+" + Math.max(0, enhancementLevel));
        lines.add("§7Эффекты:");
        for (RuneTechnicalCatalog.EffectLabel effect : effects) {
            String target = effect.channel == null ? "" : " — " + TechnicalRuneUiLabels.channel(effect.channel);
            String note = effect.runtimeNoOp ? " (§cне действует§f)" : "";
            lines.add("§f" + TechnicalRuneUiLabels.effect(effect.type) + target + note);
        }
        return lines;
    }

}
