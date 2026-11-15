package com.example.revolution.client;

import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneTechnicalCatalog;
import com.example.revolution.rune.rules.RuneProgression;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Functional v2.3 tooltip skeleton for fixed technical collections 2-7. */
final class TechnicalRuneTooltipContent {
    private TechnicalRuneTooltipContent() {}

    static String name(int collectionIndex, int runeIndex) {
        return String.format(Locale.ROOT, "Коллекция %02d / Руна %02d", collectionIndex + 1, runeIndex + 1);
    }

    static String type(int collectionIndex, int runeIndex) {
        RuneCollectionId collectionId = RuneCollectionId.fromLegacyIndex(collectionIndex);
        List<RuneTechnicalCatalog.EffectLabel> effects = RuneTechnicalCatalog.effects(collectionId, runeIndex);
        if (effects.isEmpty()) return "Нет эффекта";
        RuneTechnicalCatalog.EffectLabel first = effects.get(0);
        return label(first.type) + (first.channel == null ? "" : " — " + channel(first.channel.name()));
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
            String target = effect.channel == null ? "" : " — " + channel(effect.channel.name());
            String note = effect.runtimeNoOp ? " (§cне действует§f)" : "";
            lines.add("§f" + label(effect.type) + target + note);
        }
        return lines;
    }

    private static String channel(String raw) {
        switch (raw) {
            case "CLICK": return "клик";
            case "POISON": return "яд";
            case "BURNING": return "горение";
            case "BLEEDING": return "кровотечение";
            case "CURSE": return "проклятие";
            case "WITHER": return "иссушение";
            case "DECAY": return "разложение";
            default: return "неизвестный канал";
        }
    }

    private static String label(String type) {
        switch (type) {
            case "DAMAGE_FLAT": return "Дополнительный урон";
            case "DAMAGE_MULTIPLIER": return "Множитель урона";
            case "VULNERABILITY": return "Уязвимость";
            case "ALL_DAMAGE_MULTIPLIER": return "Множитель всего урона";
            case "TARGETED_POWER": return "Усиление выбранного эффекта";
            case "PROC_CHANCE": return "Шанс срабатывания";
            case "PROC_DAMAGE_MULTIPLIER": return "Множитель урона срабатывания";
            case "TEMPORARY_DAMAGE_MULTIPLIER": return "Временный множитель урона";
            case "MONSTER_GOLD_MULTIPLIER": return "Монеты за монстров";
            case "MONSTER_RESOURCE_MULTIPLIER": return "Ресурсы за монстров";
            case "PET_XP_MULTIPLIER": return "Опыт питомца";
            case "DELAYED_DAMAGE_BONUS": return "Отложенный дополнительный урон";
            case "EFFECTIVENESS": return "Эффективность";
            case "TEMPORARY_EFFECT_DURATION": return "Длительность временного эффекта";
            case "ENHANCEMENT_EFFECTIVENESS": return "Эффективность усиления";
            default: return "Неизвестный технический эффект";
        }
    }
}
