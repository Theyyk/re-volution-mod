package com.example.revolution.mob.definition;

import com.example.revolution.mob.rules.MobHealthRules;
import java.math.BigDecimal;
import java.util.Locale;

/** Immutable spawn configuration resolved from mob, reward and optional Location/Mob Spawn records. */
public final class ManagedMobDefinition {
    public final String name, type, resource, spawnKey;
    public final BigDecimal hp, amount;
    public final double x, y, z;

    public ManagedMobDefinition(String name, String type, BigDecimal hp, String resource, BigDecimal amount, double x, double y, double z) {
        this(name, type, hp, resource, amount, x, y, z,
                String.valueOf(name) + "|" + String.valueOf(type).toLowerCase(Locale.ROOT) + "|"
                        + Double.toString(x) + "|" + Double.toString(y) + "|" + Double.toString(z));
    }

    public ManagedMobDefinition(
            String name,
            String type,
            BigDecimal hp,
            String resource,
            BigDecimal amount,
            double x,
            double y,
            double z,
            String spawnKey) {
        if (spawnKey == null || spawnKey.trim().isEmpty()) throw new IllegalArgumentException("spawnKey is required");
        this.name = name;
        this.type = type;
        this.hp = MobHealthRules.positive(hp);
        this.resource = resource;
        this.amount = MobHealthRules.positive(amount);
        this.x = x;
        this.y = y;
        this.z = z;
        this.spawnKey = spawnKey;
    }
}
