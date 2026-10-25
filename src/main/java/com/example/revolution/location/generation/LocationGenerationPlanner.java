package com.example.revolution.location.generation;

import com.example.revolution.location.definition.MobZoneIds;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Pure deterministic planner. It has no Minecraft, persistence, or server-thread dependency. */
public final class LocationGenerationPlanner {
    public LocationGenerationPlan plan(
            LocationTemplate template,
            int worldLocationId,
            int mobSlotId,
            double anchorX,
            double anchorY,
            double anchorZ,
            long seed) {
        if (template == null) throw new IllegalArgumentException("template is required");
        if (!Double.isFinite(anchorX) || !Double.isFinite(anchorY) || !Double.isFinite(anchorZ)) {
            throw new IllegalArgumentException("anchor coordinates must be finite");
        }
        int internalId = MobZoneIds.encode(worldLocationId, mobSlotId);
        Random random = new Random(mix(seed, worldLocationId, mobSlotId, template.getTemplateId().hashCode()));
        int rotation = random.nextInt(4) * 90;
        List<LocationGenerationPlan.DecorationPlacement> decorations = new ArrayList<>();
        for (LocationDecorationKind kind : template.getDecorationKinds()) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = template.getRadius() * (0.30D + random.nextDouble() * 0.45D);
            decorations.add(new LocationGenerationPlan.DecorationPlacement(
                    kind,
                    Math.cos(angle) * distance,
                    Math.sin(angle) * distance,
                    random.nextInt(4) * 90));
        }
        return new LocationGenerationPlan(
                worldLocationId, mobSlotId, internalId, template.getTemplateId(), seed,
                anchorX, anchorY, anchorZ, template.getRadius(), rotation, decorations);
    }

    /** Stable default seed so `/stage location create L S` is reproducible at the same anchor/world. */
    public long defaultSeed(long worldSeed, int worldLocationId, int mobSlotId, int anchorX, int anchorY, int anchorZ) {
        long value = worldSeed;
        value = value * 31L + worldLocationId;
        value = value * 31L + mobSlotId;
        value = value * 31L + anchorX;
        value = value * 31L + anchorY;
        value = value * 31L + anchorZ;
        return avalanche(value);
    }

    private static long mix(long seed, int worldLocationId, int mobSlotId, int templateHash) {
        long value = seed;
        value ^= ((long) worldLocationId << 32) ^ (mobSlotId & 0xffffffffL);
        value ^= ((long) templateHash << 1);
        return avalanche(value);
    }

    private static long avalanche(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        value ^= value >>> 33;
        return value;
    }
}
