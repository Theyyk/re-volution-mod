package com.example.revolution.mob.service;

import com.example.revolution.location.definition.LocationDefinition;
import net.minecraft.util.math.MathHelper;

/**
 * Deterministic block-center fallback used after the fast radial spawn probes.
 * It guarantees that narrow but valid walkable strips are not skipped simply
 * because none of the radial sample points happened to land on their blocks.
 */
final class MobZoneSurfaceSearch {
    interface SurfaceProbe {
        Double surfaceY(double x, double z);
    }

    private MobZoneSurfaceSearch() {}

    static double[] findNearestBlockCenter(
            LocationDefinition zone,
            double preferredX,
            double preferredZ,
            SurfaceProbe probe) {
        int minX = MathHelper.floor(zone.getCenterX() - zone.getRadius());
        int maxX = MathHelper.floor(zone.getCenterX() + zone.getRadius());
        int minZ = MathHelper.floor(zone.getCenterZ() - zone.getRadius());
        int maxZ = MathHelper.floor(zone.getCenterZ() + zone.getRadius());

        double bestDistanceSq = Double.POSITIVE_INFINITY;
        double[] best = null;
        for (int blockX = minX; blockX <= maxX; blockX++) {
            double x = blockX + 0.5D;
            for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                double z = blockZ + 0.5D;
                if (!zone.contains(x, z)) continue;

                double dx = x - preferredX;
                double dz = z - preferredZ;
                double distanceSq = dx * dx + dz * dz;
                if (distanceSq >= bestDistanceSq) continue;

                Double y = probe.surfaceY(x, z);
                if (y == null) continue;
                bestDistanceSq = distanceSq;
                best = new double[] {x, y, z};
            }
        }
        return best;
    }
}
