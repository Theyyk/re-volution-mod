package com.example.revolution.mob.state;

import com.example.revolution.mob.definition.ManagedMobDefinition;
import java.math.BigDecimal;
import net.minecraft.entity.EntityLiving;

/** Mutable values owned by one managed-mob service on the server thread. Never persisted. */
public final class MobRuntimeState {
    public final ManagedMobDefinition definition;
    public BigDecimal currentHp;
    public EntityLiving entity;
    public int respawnTimer;
    public boolean spawnFailureLogged, chunkWasUnloaded, hasSpawnedOnce;

    public MobRuntimeState(ManagedMobDefinition definition) {
        this.definition = definition;
        this.currentHp = definition.hp;
    }
}
