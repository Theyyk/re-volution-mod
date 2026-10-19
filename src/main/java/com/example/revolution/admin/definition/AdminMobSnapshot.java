package com.example.revolution.admin.definition;

/** Immutable projection of a configured managed mob for external administration. */
public final class AdminMobSnapshot {
    public final String name;
    public final String type;
    public final String hp;
    public final String resource;
    public final String amount;
    public final double x;
    public final double y;
    public final double z;
    public final boolean enabled;

    public AdminMobSnapshot(String name, String type, String hp, String resource, String amount,
                            double x, double y, double z, boolean enabled) {
        this.name = name;
        this.type = type;
        this.hp = hp;
        this.resource = resource;
        this.amount = amount;
        this.x = x;
        this.y = y;
        this.z = z;
        this.enabled = enabled;
    }
}
