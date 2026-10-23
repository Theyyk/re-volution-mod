package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.repository.LocationRepository;
import com.example.revolution.mob.service.MobProgressionService;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Player/Location runtime boundary for movement between already-unlocked mob stages.
 * mobSlot remains progression/order data only; this service owns transition execution.
 */
public final class StageTransitionService {
    private static final double MAX_ENTRY_INSET = 1.5D;
    private static final double ENTRY_INSET_FACTOR = 0.25D;

    public enum Direction {
        NEXT,
        PREVIOUS
    }

    public static final class TransitionResult {
        private final int fromInternalLocationId;
        private final int toInternalLocationId;
        private final double x;
        private final double y;
        private final double z;

        TransitionResult(int fromInternalLocationId, int toInternalLocationId, double x, double y, double z) {
            this.fromInternalLocationId = fromInternalLocationId;
            this.toInternalLocationId = toInternalLocationId;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int getFromInternalLocationId() { return fromInternalLocationId; }
        public int getToInternalLocationId() { return toInternalLocationId; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
    }

    static final class HorizontalPoint {
        final double x;
        final double z;

        HorizontalPoint(double x, double z) {
            this.x = x;
            this.z = z;
        }
    }

    private final LocationRepository locations;
    private final MobProgressionService progression;

    public StageTransitionService(LocationRepository locations, MobProgressionService progression) {
        if (locations == null) throw new IllegalArgumentException("locations is required");
        if (progression == null) throw new IllegalArgumentException("progression is required");
        this.locations = locations;
        this.progression = progression;
    }

    /** Executes one explicit manual transition. Automatic edge handling is intentionally separate. */
    public TransitionResult move(EntityPlayerMP player, Direction direction) {
        if (player == null) throw new IllegalArgumentException("player is required");
        if (direction == null) throw new IllegalArgumentException("direction is required");

        LocationDefinition current = requireCurrentMobZone(player.posX, player.posZ);
        Integer targetId = direction == Direction.NEXT
                ? progression.nextUnlockedInternalLocationId(player.getUniqueID(), current.getLocationId())
                : progression.previousUnlockedInternalLocationId(player.getUniqueID(), current.getLocationId());
        if (targetId == null) {
            throw new IllegalStateException(direction == Direction.NEXT
                    ? "No unlocked next mob stage is available"
                    : "No unlocked previous mob stage is available");
        }

        LocationDefinition target = locations.getLocation(targetId);
        if (target == null || !MobZoneIds.isMobZoneId(target.getLocationId())) {
            throw new IllegalStateException("Target mob stage geometry is unavailable: " + targetId);
        }
        if (MobZoneIds.worldLocationId(target.getLocationId()) != MobZoneIds.worldLocationId(current.getLocationId())) {
            throw new IllegalStateException("Refusing cross-Location mob stage transition");
        }

        HorizontalPoint point = entryPoint(current, target, player.posX, player.posZ);
        double y = player.posY;
        player.connection.setPlayerLocation(point.x, y, point.z, player.rotationYaw, player.rotationPitch);
        player.motionX = 0.0D;
        player.motionY = 0.0D;
        player.motionZ = 0.0D;
        player.fallDistance = 0.0F;
        return new TransitionResult(current.getLocationId(), target.getLocationId(), point.x, y, point.z);
    }

    LocationDefinition requireCurrentMobZone(double x, double z) {
        List<LocationDefinition> all = locations.getLocations();
        LocationDefinition match = null;
        if (all != null) {
            for (LocationDefinition location : all) {
                if (location == null || !MobZoneIds.isMobZoneId(location.getLocationId()) || !location.contains(x, z)) continue;
                if (match != null) {
                    throw new IllegalStateException(
                            "Player position belongs to multiple mob stages: "
                                    + match.getLocationId() + " and " + location.getLocationId());
                }
                match = location;
            }
        }
        if (match == null) throw new IllegalStateException("Player is not inside a configured mob stage");
        return match;
    }

    /**
     * Places the player just inside the target circle at the side nearest the source/player.
     * The target is always strictly inside its Location; vertical placement is preserved by move().
     */
    static HorizontalPoint entryPoint(
            LocationDefinition source,
            LocationDefinition target,
            double sourceX,
            double sourceZ) {
        if (source == null || target == null) throw new IllegalArgumentException("source and target are required");

        double dx = sourceX - target.getCenterX();
        double dz = sourceZ - target.getCenterZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-9D) {
            dx = source.getCenterX() - target.getCenterX();
            dz = source.getCenterZ() - target.getCenterZ();
            length = Math.sqrt(dx * dx + dz * dz);
        }
        if (length < 1.0E-9D) {
            return new HorizontalPoint(target.getCenterX(), target.getCenterZ());
        }

        double inset = Math.min(MAX_ENTRY_INSET, target.getRadius() * ENTRY_INSET_FACTOR);
        double distance = Math.max(0.0D, target.getRadius() - inset);
        return new HorizontalPoint(
                target.getCenterX() + dx / length * distance,
                target.getCenterZ() + dz / length * distance);
    }
}
