package com.example.revolution.admin.definition;

/** Exact values and stable Location/stage identity, independent of the desktop host. */
public final class AdminStageSnapshot {
    public final int location, stage, internalId, population;
    public final String mob, hp, resource, amount, kind, problem;
    public final boolean boss, persistedBoss;
    public final double x, y, z, radius;
    public AdminStageSnapshot(int location, int stage, int internalId, int population, String mob,
            String hp, String resource, String amount, String kind, String problem,
            boolean boss, boolean persistedBoss, double x, double y, double z, double radius) {
        this.location = location; this.stage = stage; this.internalId = internalId; this.population = population;
        this.mob = mob; this.hp = hp; this.resource = resource; this.amount = amount; this.kind = kind;
        this.problem = problem; this.boss = boss; this.persistedBoss = persistedBoss;
        this.x = x; this.y = y; this.z = z; this.radius = radius;
    }
}
