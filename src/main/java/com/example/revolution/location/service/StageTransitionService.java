package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.repository.LocationRepository;
import com.example.revolution.mob.service.MobProgressionService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Player/Location runtime boundary for movement between already-unlocked mob stages.
 * mobSlot remains progression/order data only; this service owns transition execution.
 */
public final class StageTransitionService {
    private static final double SUPPORT_PROBE = 0.05D;

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

    private final LocationRepository locations;
    private final MobProgressionService progression;
    private final Map<UUID, Integer> lastStageByPlayer = new ConcurrentHashMap<>();

    public StageTransitionService(LocationRepository locations, MobProgressionService progression) {
        if (locations == null) throw new IllegalArgumentException("locations is required");
        if (progression == null) throw new IllegalArgumentException("progression is required");
        this.locations = locations;
        this.progression = progression;
    }

    /** Stores an explicit player entry point for one Location + mobSlot pair. */
    public LocationDefinition configureTransitionPoint(
            int worldLocationId,
            int mobSlotId,
            double x,
            double y,
            double z,
            float yaw,
            float pitch) {
        int internalLocationId = MobZoneIds.encode(worldLocationId, mobSlotId);
        LocationDefinition location = locations.getLocation(internalLocationId);
        if (location == null || !MobZoneIds.isMobZoneId(location.getLocationId())) {
            throw new IllegalArgumentException(
                    "Unknown mob stage: Location " + worldLocationId + ", mobSlot " + mobSlotId);
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Transition coordinates and rotation must be finite");
        }

        LocationDefinition updated = new LocationDefinition(
                location.getLocationId(),
                location.getName(),
                location.getCenterX(),
                location.getCenterZ(),
                location.getRadius(),
                x,
                y,
                z,
                yaw,
                pitch);
        locations.upsertLocation(updated);
        return updated;
    }

    /** Clears only the transition point for one Location + mobSlot pair. */
    public LocationDefinition clearTransitionPoint(int worldLocationId, int mobSlotId) {
        int internalLocationId = MobZoneIds.encode(worldLocationId, mobSlotId);
        LocationDefinition location = locations.getLocation(internalLocationId);
        if (location == null || !MobZoneIds.isMobZoneId(location.getLocationId())) {
            throw new IllegalArgumentException(
                    "Unknown mob stage: Location " + worldLocationId + ", mobSlot " + mobSlotId);
        }

        LocationDefinition updated = new LocationDefinition(
                location.getLocationId(),
                location.getName(),
                location.getCenterX(),
                location.getCenterZ(),
                location.getRadius());
        locations.upsertLocation(updated);
        return updated;
    }

    /** Executes one explicit manual transition. Automatic edge handling is intentionally separate. */
    public TransitionResult move(EntityPlayerMP player, Direction direction) {
        if (player == null) throw new IllegalArgumentException("player is required");
        if (direction == null) throw new IllegalArgumentException("direction is required");

        LocationDefinition current = resolveCurrentStage(player);
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
        int worldLocationId = MobZoneIds.worldLocationId(current.getLocationId());
        if (MobZoneIds.worldLocationId(target.getLocationId()) != worldLocationId) {
            throw new IllegalStateException("Refusing cross-Location mob stage transition");
        }
        if (!target.hasTransitionPoint()) {
            throw new IllegalStateException("Target mob stage has no transition point configured: " + targetId);
        }

        ensureSafeDestination(player, target);
        player.connection.setPlayerLocation(
                target.getTransitionX(),
                target.getTransitionY(),
                target.getTransitionZ(),
                target.getTransitionYaw(),
                target.getTransitionPitch());
        player.motionX = 0.0D;
        player.motionY = 0.0D;
        player.motionZ = 0.0D;
        player.fallDistance = 0.0F;
        lastStageByPlayer.put(player.getUniqueID(), target.getLocationId());
        return new TransitionResult(
                current.getLocationId(),
                target.getLocationId(),
                target.getTransitionX(),
                target.getTransitionY(),
                target.getTransitionZ());
    }

    /**
     * Resolves stage context even when the player is between mob-zone circles.
     * Priority: current zone -> remembered stage -> nearest configured stage point.
     */
    LocationDefinition resolveCurrentStage(EntityPlayerMP player) {
        LocationDefinition insideZone = findCurrentMobZone(player.posX, player.posZ);
        if (insideZone != null) {
            lastStageByPlayer.put(player.getUniqueID(), insideZone.getLocationId());
            return insideZone;
        }

        Integer rememberedId = lastStageByPlayer.get(player.getUniqueID());
        if (rememberedId != null && MobZoneIds.isMobZoneId(rememberedId)) {
            LocationDefinition remembered = locations.getLocation(rememberedId);
            if (remembered != null) return remembered;
        }

        LocationDefinition nearest = nearestConfiguredStage(
                locations.getLocations(), player.posX, player.posZ);
        if (nearest == null) {
            throw new IllegalStateException("Cannot resolve current mob stage outside a mob zone");
        }
        lastStageByPlayer.put(player.getUniqueID(), nearest.getLocationId());
        return nearest;
    }

    LocationDefinition findCurrentMobZone(double x, double z) {
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
        return match;
    }

    static LocationDefinition nearestConfiguredStage(List<LocationDefinition> all, double x, double z) {
        LocationDefinition best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        int bestLocation = Integer.MAX_VALUE;
        int bestSlot = Integer.MAX_VALUE;
        if (all == null) return null;

        for (LocationDefinition location : all) {
            if (location == null
                    || !MobZoneIds.isMobZoneId(location.getLocationId())
                    || !location.hasTransitionPoint()) {
                continue;
            }
            double dx = location.getTransitionX() - x;
            double dz = location.getTransitionZ() - z;
            double distance = dx * dx + dz * dz;
            int worldLocation = MobZoneIds.worldLocationId(location.getLocationId());
            int slot = MobZoneIds.mobSlotId(location.getLocationId());
            if (distance < bestDistance
                    || (distance == bestDistance && worldLocation < bestLocation)
                    || (distance == bestDistance && worldLocation == bestLocation && slot < bestSlot)) {
                best = location;
                bestDistance = distance;
                bestLocation = worldLocation;
                bestSlot = slot;
            }
        }
        return best;
    }

    private void ensureSafeDestination(EntityPlayerMP player, LocationDefinition target) {
        double dx = target.getTransitionX() - player.posX;
        double dy = target.getTransitionY() - player.posY;
        double dz = target.getTransitionZ() - player.posZ;
        AxisAlignedBB destination = player.getEntityBoundingBox().offset(dx, dy, dz);
        if (!player.world.getCollisionBoxes(player, destination).isEmpty()) {
            throw new IllegalStateException("Target mob stage transition point is obstructed: " + target.getLocationId());
        }
        if (player.world.getCollisionBoxes(player, destination.offset(0.0D, -SUPPORT_PROBE, 0.0D)).isEmpty()) {
            throw new IllegalStateException("Target mob stage transition point has no solid support: " + target.getLocationId());
        }
    }
}
