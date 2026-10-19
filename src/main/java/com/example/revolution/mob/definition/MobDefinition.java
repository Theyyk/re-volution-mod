package com.example.revolution.mob.definition;

import java.math.BigDecimal;

/** Configured managed mob, independent of a running world or entity. */
public final class MobDefinition {
    public final String name;
    public final String type;
    public final BigDecimal hp;
    public final int money;
    public final double x, y, z;
    public final boolean enabled;

    public MobDefinition(String name, String type, BigDecimal hp, int money, double x, double y, double z, boolean enabled) {
        this.name = name; this.type = type; this.hp = hp; this.money = money;
        this.x = x; this.y = y; this.z = z; this.enabled = enabled;
    }
}
