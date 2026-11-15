package com.example.revolution.network;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.unique.UniqueRuneModel;
import com.example.revolution.unique.UniqueRuneService;
import java.math.BigDecimal;
import java.util.Map;

/** Player-facing labels only. Persistence IDs, wire enums and balance values are unchanged. */
public final class UniqueRuneUiText {
    private UniqueRuneUiText() {}

    public static String runeName(String id, String fallback) {
        if (id != null) {
            String[] prefixes = {"crystal_", "daily_", "offerings_"};
            String[] labels = {"Кристальная руна", "Ежедневная руна", "Руна подношений"};
            for (int i = 0; i < prefixes.length; i++) {
                if (id.startsWith(prefixes[i]) && id.substring(prefixes[i].length()).matches("[0-9]+"))
                    return labels[i] + " №" + Integer.parseInt(id.substring(prefixes[i].length()));
            }
        }
        return fallback == null || fallback.isEmpty() ? "Неизвестная руна" : fallback;
    }

    public static String collection(UniqueRuneModel.CollectionId id) {
        if (id == null) return "Неизвестная коллекция";
        switch (id) {
            case CRYSTAL_COLLECTION: return "Кристальные руны";
            case DAILY_COLLECTION: return "Ежедневные руны";
            case OFFERINGS_COLLECTION: return "Руны подношений";
            default: return "Неизвестная коллекция";
        }
    }

    public static String rarity(UniqueRuneModel.Rarity rank) {
        if (rank == null) return "Неизвестный ранг";
        switch (rank) {
            case COMMON: return "Обычная";
            case IMPROVED: return "Улучшенная";
            case ADVANCED: return "Продвинутая";
            case MYTHIC: return "Мифическая";
            case ANCIENT: return "Древняя";
            default: return "Неизвестный ранг";
        }
    }

    public static String effect(String key) {
        if (key == null) return "Неизвестный эффект";
        switch (key) {
            case "CLICK_DAMAGE_MULTIPLIER": return "Урон клика";
            case "POISON_DAMAGE_MULTIPLIER": return "Урон яда";
            case "BURNING_DAMAGE_MULTIPLIER": return "Урон горения";
            case "BLEEDING_DAMAGE_MULTIPLIER": return "Урон кровотечения";
            case "CURSE_DAMAGE_MULTIPLIER": return "Урон проклятия";
            case "WITHER_DAMAGE_MULTIPLIER": return "Урон иссушения";
            case "DECAY_DAMAGE_MULTIPLIER": return "Урон разложения";
            case "ALL_DAMAGE_MULTIPLIER": return "Весь урон";
            case "VULNERABILITY_MULTIPLIER": return "Уязвимость";
            case "EFFECTIVENESS_MULTIPLIER": return "Эффективность";
            case "CLICK_CRIT_CHANCE_MULTIPLIER": return "Шанс критического удара";
            case "CLICK_CRIT_DAMAGE_MULTIPLIER": return "Критический урон";
            case "COIN_REWARD_MULTIPLIER": return "Награда монетами";
            case "RUNE_RESOURCE_REWARD_MULTIPLIER": return "Рунные ресурсы";
            case "RUNE_DUST_REWARD_MULTIPLIER": return "Рунная пыль";
            case "RUNE_SHARDS_REWARD_MULTIPLIER": return "Рунные осколки";
            case "RUNE_CORES_REWARD_MULTIPLIER": return "Рунные ядра";
            case "PET_XP_MULTIPLIER": return "Опыт питомцев";
            case "SPECIAL_EFFECT_MULTIPLIER": return "Особые эффекты";
            case "FORGE_RESOURCE_MULTIPLIER": return "Ресурсы кузницы";
            case "FORGE_SPEED_MULTIPLIER": return "Скорость кузницы";
            default: return "Неизвестный технический эффект";
        }
    }

    public static String effects(Map<String, String> effects) {
        if (effects == null || effects.isEmpty()) return "Нет";
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : effects.entrySet()) {
            if (out.length() > 0) out.append(", ");
            out.append(effect(entry.getKey())).append(" +").append(entry.getValue());
        }
        return out.toString();
    }

    public static String notification(String action, UniqueRuneService.Result result,
                                      String runeName, int enhancementLevel) {
        if (result == null || result.code == UniqueRuneService.ResultCode.REPLAYED) return null;
        if (!"purchase_crystal".equals(action) && !"enhance".equals(action)) return null;
        if (result.ok()) {
            if ("enhance".equals(action)) return "Усиление: " + runeName + " (+" + enhancementLevel + ")";
            if (result.duplicate) return "Дубликат: " + runeName + ". Компенсация: "
                    + exact(result.compensation) + " жемчуга";
            return "Получена " + runeName;
        }
        switch (result.code) {
            case INSUFFICIENT_RESOURCE: return "Недостаточно ресурсов для операции с руной";
            case COMPLETE: return "Все руны этой коллекции уже получены";
            case LOCKED: return "Коллекция рун пока недоступна";
            default: return null;
        }
    }

    private static String exact(BigDecimal amount) {
        return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
    }
}
