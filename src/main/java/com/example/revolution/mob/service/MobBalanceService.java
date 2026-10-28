package com.example.revolution.mob.service;

import com.example.revolution.balance.definition.BalanceConfig;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.service.BalanceSnapshot;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.resource.definition.ResourceDefinitions;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves optional canonical MobL_N HP/reward overrides from the active Balance Core snapshot. */
public final class MobBalanceService {
    private static final Pattern CANONICAL_MOB = Pattern.compile("^Mob([1-9][0-9]*)_([1-9][0-9]*)$");

    private final BalanceService balance;

    public MobBalanceService(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance is required");
        this.balance = balance;
    }

    public BigDecimal hp(String mobName, BigDecimal fallback) {
        String raw = value(mobName, "hp");
        return raw == null ? fallback : positive(raw, key(mobName, "hp"));
    }

    public String resource(String mobName, String fallback) {
        String raw = value(mobName, "resource");
        if (raw == null) return fallback;
        String canonical = ResourceDefinitions.findCanonicalName(raw);
        if (canonical == null) throw new IllegalArgumentException("unknown mob balance resource '" + raw + "' for " + mobName);
        return canonical;
    }

    public BigDecimal reward(String mobName, BigDecimal fallback) {
        String raw = value(mobName, "reward");
        return raw == null ? fallback : positive(raw, key(mobName, "reward"));
    }

    public boolean hasAnyOverride(String mobName) {
        return value(mobName, "hp") != null || value(mobName, "resource") != null || value(mobName, "reward") != null;
    }

    private String value(String mobName, String field) {
        String key = key(mobName, field);
        if (key == null) return null;
        BalanceSnapshot snapshot = balance.getActiveSnapshot();
        if (snapshot == null) return null;
        BalanceConfig config = snapshot.getConfig();
        return config.getValue(key);
    }

    private String key(String mobName, String field) {
        Matcher matcher = mobName == null ? null : CANONICAL_MOB.matcher(mobName);
        if (matcher == null || !matcher.matches()) return null;
        return "mobs.location." + matcher.group(1) + ".slot." + matcher.group(2) + "." + field;
    }

    private BigDecimal positive(String raw, String key) {
        try {
            BigDecimal value = LargeNumbers.parseExact(raw);
            if (value.signum() <= 0) throw new IllegalArgumentException("must be > 0");
            return value;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("invalid mob balance value '" + key + "' = '" + raw + "'", e);
        }
    }
}
