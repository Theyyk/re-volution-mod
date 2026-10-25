package com.example.revolution.mob.state;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobSpawnDefinition;
import com.example.revolution.mob.definition.ManagedMobDefinition;
import java.math.BigDecimal;
import net.minecraft.entity.EntityLiving;

/** Mutable values owned by one managed-mob service on the server thread. Never persisted. */
public final class MobRuntimeState {
    public final ManagedMobDefinition definition;
    public final int realmId;
    public final int dimensionId;
    public final LocationDefinition location;
    public final MobSpawnDefinition spawnDefinition;
    public final MobContributionLedger contributions = new MobContributionLedger();
    public BigDecimal currentHp;
    public EntityLiving entity;
    public int respawnTimer;
    public int roamCooldown;
    public int separationCooldown;
    public int combatPauseTicks;
    public int deathVisualTicks;
    public boolean spawnFailureLogged, chunkWasUnloaded, hasSpawnedOnce, initialResolved;
    public double initialX, initialY, initialZ;

    /** Legacy Realm 0 fixed/stationary spawn. */
    public MobRuntimeState(ManagedMobDefinition definition) {
        this(definition, 0, 0, null, null);
        this.initialX = definition.x;
        this.initialY = definition.y;
        this.initialZ = definition.z;
        this.initialResolved = true;
    }

    public MobRuntimeState(
            ManagedMobDefinition definition,
            int realmId,
            int dimensionId,
            LocationDefinition location,
            MobSpawnDefinition spawnDefinition) {
        this.definition = definition;
        this.realmId = realmId;
        this.dimensionId = dimensionId;
        this.location = location;
        this.spawnDefinition = spawnDefinition;
        this.currentHp = definition.hp;
    }

    public boolean isLegacy() { return spawnDefinition == null || location == null; }
}
