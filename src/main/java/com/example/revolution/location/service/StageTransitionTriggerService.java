package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationStageTemplate;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition.DisplayMode;
import com.example.revolution.location.repository.LocationRepository;
import com.example.revolution.location.repository.StageTransitionPreferenceRepository;
import com.example.revolution.location.repository.StageTransitionTriggerRepository;
import com.example.revolution.mob.service.MobProgressionService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

/** Runtime for manual 180/180 stage boundary fallback rings, delayed transitions, and data-driven messages. */
public final class StageTransitionTriggerService {
    private static final long TRANSITION_DELAY_TICKS = 20L;
    private static final long TELEPORT_COOLDOWN_TICKS = 10L;

    public static final class OverlayGeometry {
        private final double innerRadius;
        private final double outerRadius;
        private final double forwardX;
        private final double forwardZ;

        OverlayGeometry(double innerRadius, double outerRadius, double forwardX, double forwardZ) {
            this.innerRadius = innerRadius;
            this.outerRadius = outerRadius;
            this.forwardX = forwardX;
            this.forwardZ = forwardZ;
        }

        public double getInnerRadius() { return innerRadius; }
        public double getOuterRadius() { return outerRadius; }
        public double getForwardX() { return forwardX; }
        public double getForwardZ() { return forwardZ; }
    }

    private final StageTransitionTriggerRepository repository;
    private final StageTransitionPreferenceRepository preferences;
    private final LocationRepository locations;
    private final MobProgressionService progression;
    private final StageTransitionService transitions;
    private final Map<String, StageTransitionTriggerDefinition> triggers = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> insideByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldownUntilTick = new ConcurrentHashMap<>();
    private final Map<UUID, PendingTransition> pendingByPlayer = new ConcurrentHashMap<>();

    public StageTransitionTriggerService(
            StageTransitionTriggerRepository repository,
            StageTransitionPreferenceRepository preferences,
            LocationRepository locations,
            MobProgressionService progression,
            StageTransitionService transitions) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        if (preferences == null) throw new IllegalArgumentException("preferences is required");
        if (locations == null) throw new IllegalArgumentException("locations is required");
        if (progression == null) throw new IllegalArgumentException("progression is required");
        if (transitions == null) throw new IllegalArgumentException("transitions is required");
        this.repository = repository;
        this.preferences = preferences;
        this.locations = locations;
        this.progression = progression;
        this.transitions = transitions;
    }

    /** Cache an already committed new stage without reloading mobs or progression configuration. */
    public void rememberCreatedTrigger(int worldLocationId, int mobSlotId, LocationStageTemplate template) {
        StageTransitionTriggerDefinition trigger = new StageTransitionTriggerDefinition(
                worldLocationId, mobSlotId, template.getTriggerOffset(), template.getTriggerRadius(),
                DisplayMode.ACTION_BAR, null, null, null);
        triggers.put(trigger.key(), trigger);
    }

    public void reload() {
        triggers.clear();
        insideByPlayer.clear();
        pendingByPlayer.clear();
        cooldownUntilTick.clear();
        List<StageTransitionTriggerDefinition> stored = repository.getAll();
        if (stored == null) return;
        for (StageTransitionTriggerDefinition trigger : stored) {
            if (trigger != null) triggers.put(trigger.key(), trigger);
        }
    }

    public StageTransitionTriggerDefinition configureTrigger(
            int worldLocationId, int sourceMobSlotId, double offset, double radius) {
        int sourceId = MobZoneIds.encode(worldLocationId, sourceMobSlotId);
        LocationDefinition source = locations.getLocation(sourceId);
        if (source == null || !MobZoneIds.isMobZoneId(source.getLocationId())) {
            throw new IllegalArgumentException("Unknown mob stage: Location " + worldLocationId + ", mobSlot " + sourceMobSlotId);
        }
        StageTransitionTriggerDefinition previous = triggers.get(key(worldLocationId, sourceMobSlotId));
        StageTransitionTriggerDefinition configured = new StageTransitionTriggerDefinition(
                worldLocationId, sourceMobSlotId, offset, radius,
                previous == null ? DisplayMode.ACTION_BAR : previous.getDisplayMode(),
                previous == null ? null : previous.getLockedMessage(),
                previous == null ? null : previous.getForwardMessage(),
                previous == null ? null : previous.getBackwardMessage());
        repository.upsert(configured);
        triggers.put(configured.key(), configured);
        return configured;
    }

    public void clearTrigger(int worldLocationId, int sourceMobSlotId) {
        repository.delete(worldLocationId, sourceMobSlotId);
        String removedKey = key(worldLocationId, sourceMobSlotId);
        triggers.remove(removedKey);
        for (Set<String> inside : insideByPlayer.values()) inside.remove(removedKey);
        pendingByPlayer.entrySet().removeIf(entry -> removedKey.equals(entry.getValue().triggerKey));
    }

    public StageTransitionTriggerDefinition setMessage(int worldLocationId, int sourceMobSlotId, String kind, String message) {
        StageTransitionTriggerDefinition trigger = requireTrigger(worldLocationId, sourceMobSlotId);
        String locked = trigger.getLockedMessage();
        String forward = trigger.getForwardMessage();
        String backward = trigger.getBackwardMessage();
        if ("locked".equals(kind)) locked = message;
        else if ("forward".equals(kind)) forward = message;
        else if ("backward".equals(kind)) backward = message;
        else throw new IllegalArgumentException("Message kind must be locked, forward or backward");
        StageTransitionTriggerDefinition updated = trigger.withMessages(locked, forward, backward);
        repository.upsert(updated);
        triggers.put(updated.key(), updated);
        return updated;
    }

    public StageTransitionTriggerDefinition clearMessage(int worldLocationId, int sourceMobSlotId, String kind) {
        return setMessage(worldLocationId, sourceMobSlotId, kind, null);
    }

    public StageTransitionTriggerDefinition setDisplayMode(int worldLocationId, int sourceMobSlotId, DisplayMode mode) {
        StageTransitionTriggerDefinition trigger = requireTrigger(worldLocationId, sourceMobSlotId);
        StageTransitionTriggerDefinition updated = trigger.withDisplayMode(mode);
        repository.upsert(updated);
        triggers.put(updated.key(), updated);
        return updated;
    }

    public OverlayGeometry overlayGeometry(int worldLocationId, int sourceMobSlotId) {
        StageTransitionTriggerDefinition trigger = triggers.get(key(worldLocationId, sourceMobSlotId));
        if (trigger == null) return null;
        LocationDefinition source = sourceStage(trigger);
        if (source == null) return null;
        double inner = source.getRadius() + trigger.getOffset();
        double outer = inner + trigger.getRadius();
        AdjacentStages adjacent = adjacentStages(worldLocationId, sourceMobSlotId);
        double[] axis = forwardAxis(source, adjacent);
        return axis == null ? new OverlayGeometry(inner, outer, 0.0D, 0.0D)
                : new OverlayGeometry(inner, outer, axis[0], axis[1]);
    }

    /** This preference controls fast-auto; boundary fallback rings remain available regardless of it. */
    public boolean isAutoTransitionEnabled(UUID playerUuid) { return preferences.isAutoTransitionEnabled(playerUuid); }

    public void setAutoTransitionEnabled(UUID playerUuid, boolean enabled) {
        preferences.setAutoTransitionEnabled(playerUuid, enabled);
    }

    public void handlePlayerTick(EntityPlayerMP player) {
        if (player == null || player.world == null || player.world.isRemote) return;
        UUID playerId = player.getUniqueID();
        List<StageTransitionTriggerDefinition> ordered = orderedTriggers();
        Set<String> previousInside = insideByPlayer.get(playerId);
        if (previousInside == null) previousInside = Collections.emptySet();
        Set<String> nowInside = new HashSet<>();
        List<StageTransitionTriggerDefinition> entered = new ArrayList<>();
        for (StageTransitionTriggerDefinition trigger : ordered) {
            LocationDefinition source = sourceStage(trigger);
            if (source == null || !trigger.containsBoundaryRing(source, player.posX, player.posZ)) continue;
            nowInside.add(trigger.key());
            if (!previousInside.contains(trigger.key())) entered.add(trigger);
        }
        long now = player.world.getTotalWorldTime();

        if (processPending(player, playerId, now)) {
            if (pendingByPlayer.containsKey(playerId)) insideByPlayer.put(playerId, nowInside);
            else insideByPlayer.remove(playerId);
            return;
        }

        Long cooldown = cooldownUntilTick.get(playerId);
        if (cooldown != null && now < cooldown) {
            // Do not consume a target-ring entry during teleport cooldown. After cooldown expires the
            // ring must still be considered newly entered so backward/forward travel can fire normally.
            insideByPlayer.remove(playerId);
            return;
        }
        if (cooldown != null) cooldownUntilTick.remove(playerId);

        insideByPlayer.put(playerId, nowInside);
        if (entered.isEmpty() || pendingByPlayer.containsKey(playerId)) return;

        for (StageTransitionTriggerDefinition trigger : entered) {
            LocationDefinition source = sourceStage(trigger);
            if (source == null) continue;
            AdjacentStages adjacent = adjacentStages(trigger.getWorldLocationId(), trigger.getSourceMobSlotId());
            double[] forwardAxis = forwardAxis(source, adjacent);
            if (forwardAxis == null) continue;
            boolean forwardHalf = trigger.isForwardHalf(source, player.posX, player.posZ, forwardAxis[0], forwardAxis[1]);
            Integer targetId = forwardHalf ? adjacent.nextId : adjacent.previousId;
            if (targetId == null) continue;
            if (!progression.isUnlocked(playerId, targetId)) {
                if (forwardHalf) sendConfiguredMessage(player, trigger.getLockedMessage(), trigger.getDisplayMode());
                continue;
            }
            sendConfiguredMessage(player,
                    forwardHalf ? trigger.getForwardMessage() : trigger.getBackwardMessage(), trigger.getDisplayMode());
            pendingByPlayer.put(playerId, new PendingTransition(trigger.key(), targetId, now + TRANSITION_DELAY_TICKS));
            break;
        }
    }

    /**
     * Once the player has entered a valid boundary ring, keep that transition armed for the configured delay.
     * Requiring the player to remain inside the annulus for the whole second made normal walking/sprinting
     * cross narrow trigger rings before the delay expired, so the pending transition was silently cancelled.
     */
    private boolean processPending(EntityPlayerMP player, UUID playerId, long now) {
        PendingTransition pending = pendingByPlayer.get(playerId);
        if (pending == null) return false;
        if (!triggers.containsKey(pending.triggerKey)) {
            pendingByPlayer.remove(playerId);
            return false;
        }
        if (now < pending.executeAtTick) return true;
        if (!progression.isUnlocked(playerId, pending.targetInternalLocationId)) {
            pendingByPlayer.remove(playerId);
            return false;
        }
        try {
            transitions.moveToStage(player, pending.targetInternalLocationId);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            pendingByPlayer.remove(playerId);
            player.sendStatusMessage(new TextComponentString("Переход не выполнен: " + failure.getMessage()), true);
            return false;
        }
        pendingByPlayer.remove(playerId);
        cooldownUntilTick.put(playerId, now + TELEPORT_COOLDOWN_TICKS);
        return true;
    }

    public void forgetPlayer(UUID playerUuid) {
        if (playerUuid == null) return;
        insideByPlayer.remove(playerUuid);
        cooldownUntilTick.remove(playerUuid);
        pendingByPlayer.remove(playerUuid);
    }

    private LocationDefinition sourceStage(StageTransitionTriggerDefinition trigger) {
        if (trigger == null) return null;
        return locations.getLocation(MobZoneIds.encode(trigger.getWorldLocationId(), trigger.getSourceMobSlotId()));
    }

    private AdjacentStages adjacentStages(int worldLocationId, int sourceMobSlotId) {
        int sourceId = MobZoneIds.encode(worldLocationId, sourceMobSlotId);
        return new AdjacentStages(
                progression.previousConfiguredInternalLocationId(sourceId),
                progression.nextConfiguredInternalLocationId(sourceId));
    }

    /** Forward points toward the next configured stage, including the first stage of the next Location. */
    private double[] forwardAxis(LocationDefinition source, AdjacentStages adjacent) {
        if (adjacent.nextId != null) {
            LocationDefinition next = locations.getLocation(adjacent.nextId);
            if (next != null) {
                double x = next.getCenterX() - source.getCenterX();
                double z = next.getCenterZ() - source.getCenterZ();
                if (x * x + z * z > 1.0E-12D) return new double[] { x, z };
            }
        }
        if (adjacent.previousId != null) {
            LocationDefinition previous = locations.getLocation(adjacent.previousId);
            if (previous != null) {
                double x = source.getCenterX() - previous.getCenterX();
                double z = source.getCenterZ() - previous.getCenterZ();
                if (x * x + z * z > 1.0E-12D) return new double[] { x, z };
            }
        }
        return null;
    }

    private StageTransitionTriggerDefinition requireTrigger(int worldLocationId, int sourceMobSlotId) {
        StageTransitionTriggerDefinition trigger = triggers.get(key(worldLocationId, sourceMobSlotId));
        if (trigger == null) throw new IllegalStateException(
                "Transition trigger is not configured for Location " + worldLocationId + ", mobSlot " + sourceMobSlotId);
        return trigger;
    }

    private List<StageTransitionTriggerDefinition> orderedTriggers() {
        List<StageTransitionTriggerDefinition> ordered = new ArrayList<>(triggers.values());
        ordered.sort(Comparator.comparingInt(StageTransitionTriggerDefinition::getWorldLocationId)
                .thenComparingInt(StageTransitionTriggerDefinition::getSourceMobSlotId));
        return ordered;
    }

    private void sendConfiguredMessage(EntityPlayerMP player, String message, DisplayMode mode) {
        if (message == null || message.trim().isEmpty()) return;
        TextComponentString component = new TextComponentString(message);
        if (mode == DisplayMode.CHAT) player.sendMessage(component);
        else player.sendStatusMessage(component, true);
    }

    private String key(int worldLocationId, int sourceMobSlotId) { return worldLocationId + ":" + sourceMobSlotId; }

    private static final class AdjacentStages {
        final Integer previousId;
        final Integer nextId;
        AdjacentStages(Integer previousId, Integer nextId) { this.previousId = previousId; this.nextId = nextId; }
    }

    private static final class PendingTransition {
        final String triggerKey;
        final int targetInternalLocationId;
        final long executeAtTick;
        PendingTransition(String triggerKey, int targetInternalLocationId, long executeAtTick) {
            this.triggerKey = triggerKey;
            this.targetInternalLocationId = targetInternalLocationId;
            this.executeAtTick = executeAtTick;
        }
    }
}
