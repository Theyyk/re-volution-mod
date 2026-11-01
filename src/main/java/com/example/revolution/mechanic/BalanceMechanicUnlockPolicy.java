package com.example.revolution.mechanic;

import com.example.revolution.balance.service.BalanceService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Data-driven v2.12.0 unlock catalog backed by Balance Core. */
public final class BalanceMechanicUnlockPolicy {
    public static final String ARTS = "ARTS";
    public static final String RUN_EXCHANGE = "RUN_EXCHANGE";
    public static final String GOD_WORSHIP = "GOD_WORSHIP";
    public static final String AURA_SYSTEM = "AURA_SYSTEM";
    public static final String ACHIEVEMENTS = "ACHIEVEMENTS";
    public static final String UNIQUE_RUNE_COLLECTIONS = "UNIQUE_RUNE_COLLECTIONS";
    public static final String GOD_AUTO_WORSHIP = "GOD_AUTO_WORSHIP";
    public static final String GOD_PERMANENT_DURATION = "GOD_PERMANENT_DURATION";
    public static final String GOD_SECONDARY_BUFFS = "GOD_SECONDARY_BUFFS";
    public static final String GOD_RESET_PRESERVATION = "GOD_RESET_PRESERVATION";

    private static final String[] IDS = {
            ARTS, RUN_EXCHANGE, GOD_WORSHIP, AURA_SYSTEM, ACHIEVEMENTS, UNIQUE_RUNE_COLLECTIONS,
            GOD_AUTO_WORSHIP, GOD_PERMANENT_DURATION, GOD_SECONDARY_BUFFS, GOD_RESET_PRESERVATION
    };

    private final Map<String, MechanicUnlockModel.Definition> definitions;
    private final String zbtStatus;

    public BalanceMechanicUnlockPolicy(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance is required");
        zbtStatus = value(balance, "mechanic_unlock.zbt_status");
        Map<String, MechanicUnlockModel.Definition> loaded = new LinkedHashMap<>();
        for (String id : IDS) {
            String key = "mechanic_unlock." + id.toLowerCase(java.util.Locale.ROOT) + ".";
            MechanicUnlockModel.Definition definition = new MechanicUnlockModel.Definition(
                    id,
                    Boolean.parseBoolean(value(balance, key + "enabled")),
                    value(balance, key + "target_mechanic"),
                    optionalValue(balance, key + "parent"),
                    enumValue(MechanicUnlockModel.Visibility.class, value(balance, key + "visibility")),
                    enumValue(MechanicUnlockModel.RequirementType.class, value(balance, key + "requirement_type")),
                    enumValue(MechanicUnlockModel.Rarity.class, value(balance, key + "rarity")),
                    integer(balance, key + "target_count"),
                    integer(balance, key + "display_order"),
                    value(balance, key + "display_name"));
            loaded.put(id, definition);
        }
        definitions = Collections.unmodifiableMap(loaded);
    }

    public MechanicUnlockModel.Definition definition(String id) { return definitions.get(id); }
    public String zbtStatus() { return zbtStatus; }

    public List<MechanicUnlockModel.Definition> all() {
        List<MechanicUnlockModel.Definition> result = new ArrayList<>(definitions.values());
        result.sort(Comparator.comparingInt(d -> d.order));
        return result;
    }

    private static String value(BalanceService balance, String key) { return balance.getActiveValue(key); }
    private static String optionalValue(BalanceService balance, String key) {
        String value = balance.getActiveSnapshot().getConfig().getValue(key);
        return value == null ? "" : value;
    }
    private static int integer(BalanceService balance, String key) {
        try { return Integer.parseInt(value(balance, key)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("invalid integer balance value '" + key + "'", e); }
    }
    private static <T extends Enum<T>> T enumValue(Class<T> type, String raw) {
        try { return Enum.valueOf(type, raw); }
        catch (RuntimeException e) { throw new IllegalArgumentException("invalid " + type.getSimpleName() + " '" + raw + "'", e); }
    }
}
