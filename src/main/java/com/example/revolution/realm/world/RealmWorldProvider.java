package com.example.revolution.realm.world;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.IChunkGenerator;

/** Shared provider for all RE:volution Realm dimensions. */
public final class RealmWorldProvider extends WorldProvider {
    @Override public DimensionType getDimensionType() {
        return RealmDimensions.typeForDimension(getDimension());
    }

    @Override public IChunkGenerator createChunkGenerator() {
        return new RealmVoidChunkGenerator(world);
    }
}
