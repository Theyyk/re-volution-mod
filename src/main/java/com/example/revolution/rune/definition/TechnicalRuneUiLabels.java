package com.example.revolution.rune.definition;

/** Neutral Russian presentation for technical collections, without adding any gameplay content. */
public final class TechnicalRuneUiLabels {
    private TechnicalRuneUiLabels() {}

    public static String runeName(int collectionIndex, int runeIndex) {
        return String.format(java.util.Locale.ROOT, "Коллекция %02d / Руна %02d",
                collectionIndex + 1, runeIndex + 1);
    }

    public static String channel(RuneDamageChannel channel) {
        if (channel == null) return "";
        switch (channel) {
            case CLICK: return "клик";
            case POISON: return "яд";
            case BURNING: return "горение";
            case BLEEDING: return "кровотечение";
            case CURSE: return "проклятие";
            case WITHER: return "иссушение";
            case DECAY: return "разложение";
            default: return "неизвестный канал";
        }
    }

    public static String effect(String type) {
        if (type == null) return "Неизвестный технический эффект";
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
