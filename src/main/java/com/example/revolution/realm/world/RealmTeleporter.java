package com.example.revolution.realm.world;

import com.example.revolution.realm.definition.RealmDefinition;
import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

/** Portal-free transfer that always lands at the Realm start point. */
public final class RealmTeleporter extends Teleporter {
    private final RealmDefinition realm;

    public RealmTeleporter(WorldServer world, RealmDefinition realm) {
        super(world);
        this.realm = realm;
    }

    @Override public void placeInPortal(Entity entity, float rotationYaw) {
        entity.setLocationAndAngles(
                realm.getStartX() + 0.5D,
                realm.getStartY(),
                realm.getStartZ() + 0.5D,
                entity.rotationYaw,
                entity.rotationPitch
        );
        entity.motionX = 0.0D;
        entity.motionY = 0.0D;
        entity.motionZ = 0.0D;
    }
}
