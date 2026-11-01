package com.example.revolution.resource.definition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Fixed resource names and lookup policy, shared by client and server. */
public final class ResourceDefinitions {
    private ResourceDefinitions() {}
    public static final String RUNE_DUST = "rune_dust";
    public static final String RUNE_SHARDS = "rune_shards";
    public static final String RUNE_CORES = "rune_cores";
    public static final String AURA_KEY = "aura_key";
    public static final String PEARL = "pearl";
    public static final String RETIRED_LIGHTNINGS = "lightnings";

    private static final List<String> BUILT_IN = Arrays.asList(
            "coins", "crystals", RUNE_DUST, RUNE_SHARDS, RUNE_CORES, AURA_KEY, PEARL
    );

    public static boolean isRetiredResource(String name) { return RETIRED_LIGHTNINGS.equals(normalize(name).trim()); }

    public static String normalize(String name) { return name == null ? "" : name.toLowerCase(Locale.ROOT); }

    public static String getDisplayName(String resource) {
        String normalized = normalize(resource);
        if (normalized.equals("coins")) return "Монеты";
        if (normalized.equals("crystals")) return "Кристаллы";
        if (normalized.equals(RUNE_DUST)) return "Пыль рун";
        if (normalized.equals(RUNE_SHARDS)) return "Осколки рун";
        if (normalized.equals(RUNE_CORES)) return "Ядра рун";
        if (normalized.equals(AURA_KEY)) return "Ключ Ауры";
        if (normalized.equals(PEARL)) return "Жемчуг";
        return resource;
    }

    public static List<String> getResourceNames() { return new ArrayList<>(BUILT_IN); }

    public static String findCanonicalName(String input) {
        if (input == null || isRetiredResource(input)) return null;
        for (String name : BUILT_IN) if (name.equalsIgnoreCase(input)) return name;
        return null;
    }
}
