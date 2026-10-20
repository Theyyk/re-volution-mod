package com.example.revolution.realm.world;

import com.example.revolution.realm.definition.RealmCatalog;
import com.example.revolution.realm.definition.RealmDefinition;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;

/** Registers the three Realm dimensions on both physical sides. */
public final class RealmDimensions {
    private static final Map<Integer, DimensionType> TYPES = new HashMap<>();
    private static boolean registered;

    private RealmDimensions() {}

    public static synchronized void register() {
        if (registered) return;
        for (RealmDefinition realm : RealmCatalog.all()) {
            int dimensionId = realm.getDimensionId();
            if (DimensionManager.isDimensionRegistered(dimensionId)) {
                throw new IllegalStateException("Forge dimension ID is already registered: " + dimensionId);
            }
            DimensionType type = DimensionType.register(
                    "revolution_realm_" + realm.getRealmId(),
                    "_revolution_realm_" + realm.getRealmId(),
                    dimensionId,
                    RealmWorldProvider.class,
                    false
            );
            TYPES.put(dimensionId, type);
            DimensionManager.registerDimension(dimensionId, type);
        }
        registered = true;
    }

    public static DimensionType typeForDimension(int dimensionId) {
        DimensionType type = TYPES.get(dimensionId);
        if (type == null) throw new IllegalStateException("Unknown RE:volution Realm dimension: " + dimensionId);
        return type;
    }
}
