package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
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

/** Runtime for 180/180 stage boundary rings, data-driven messages, and per-player auto preference. */
public final class StageTransitionTriggerService {
    private static final long TELEPORT_COOLDOWN_TICKS = 10L;

    private final StageTransitionTriggerRepository repository;
    private final StageTransitionPreferenceRepository preferences;
    private final LocationRepository locations;
    private final MobProgressionService progression;
    private final StageTransitionService transitions;
    private final Map<String, StageTransitionTriggerDefinition> triggers = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> insideByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldownUntilTick = new ConcurrentHashMap<>();

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

    public void reload() {
        triggers.clear();
        List<StageTransitionTriggerDefinition> stored = repository.getAll();
        if (stored == null) return;
        for (StageTransitionTriggerDefinition trigger : stored) {
            if (trigger != null) triggers.put(trigger.key(), trigger);
        }
    }

    /** Configures one outer ring around the source mob-zone. The ring is split into forward/backward halves. */
    public StageTransitionTriggerDefinition configureTrigger(int worldLocationId, int sourceMobSlotId, double width) {
        int sourceId = MobZoneIds.encode(worldLocationId, sourceMobSlotId);
        LocationDefinition source = locations.getLocation(sourceId);
        if (source == null || !MobZoneIds.isMobZoneId(source.getLocationId())) {
            throw new IllegalArgumentException(
                    "Unknown mob stage: Location " + worldLocationId + ", mobSlot " + sourceMobSlotId);
        }
        StageTransitionTriggerDefinition previous = triggers.get(key(worldLocationId, sourceMobSlotId));
        StageTransitionTriggerDefinition configured = new StageTransitionTriggerDefinition(
                worldLocationId,
                sourceMobSlotId,
                width,
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
    }

    public StageTransitionTriggerDefinition setMessage(
            int worldLocationId, int sourceMobSlotId, String kind, String message) {
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

    public StageTransitionTriggerDefinition setDisplayMode(
            int worldLocationId, int sourceMobSlotId, DisplayMode mode) {
        StageTransitionTriggerDefinition trigger = requireTrigger(worldLocationId, sourceMobSlotId);
        StageTransitionTriggerDefinition updated = trigger.withDisplayMode(mode);
        repository.upsert(updated);
        triggers.put(updated.key(), updated);
        return updated;
    }

    public boolean isAutoTransitionEnabled(UUID playerUuid) {
        return preferences.isAutoTransitionEnabled(playerUuid);
    }

    public void setAutoTransitionEnabled(UUID playerUuid, boolean enabled) {
        preferences.setAutoTransitionEnabled(playerUuid, enabled);
    }

    /** Called on the END phase of the server player tick. */
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
        insideByPlayer.put(playerId, nowInside);
        if (entered.isEmpty()) return;

        long now = player.world.getTotalWorldTime();
        Long cooldown = cooldownUntilTick.get(playerId);
        if (cooldown != null && now < cooldown) return;

        for (StageTransitionTriggerDefinition trigger : entered) {
            LocationDefinition source = sourceStage(trigger);
            if (source == null) continue;
            AdjacentStages adjacent = adjacentStages(trigger.getWorldLocationId(), trigger.getSourceMobSlotId());
            double[] forwardAxis = forwardAxis(source, adjacent);
            if (forwardAxis == null) continue;

            boolean forwardHalf = trigger.isForwardHalf(
                    source, player.posX, player.posZ, forwardAxis[0], forwardAxis[1]);
            Integer targetId = forwardHalf ? adjacent.nextId : adjacent.previousId;
            if (targetId == null) continue;

            if (!progression.isUnlocked(playerId, targetId)) {
                if (forwardHalf) sendConfiguredMessage(player, trigger.getLockedMessage(), trigger.getDisplayMode());
                continue;
            }
            if (!preferences.isAutoTransitionEnabled(playerId)) continue;

            try {
                transitions.moveToStage(player, targetId);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                continue;
            }
            sendConfiguredMessage(
                    player,
                    forwardHalf ? trigger.getForwardMessage() : trigger.getBackwardMessage(),
                    trigger.getDisplayMode());
            cooldownUntilTick.put(playerId, now + TELEPORT_COOLDOWN_TICKS);
            break;
        }
    }

    public void forgetPlayer(UUID playerUuid) {
        if (playerUuid == null) return;
        insideByPlayer.remove(playerUuid);
        cooldownUntilTick.remove(playerUuid);
    }

    private LocationDefinition sourceStage(StageTransitionTriggerDefinition trigger) {
        if (trigger == null) return null;
        return locations.getLocation(MobZoneIds.encode(
                trigger.getWorldLocationId(), trigger.getSourceMobSlotId()));
    }

    private AdjacentStages adjacentStages(int worldLocationId, int sourceMobSlotId) {
        List<Integer> configured = progression.configuredSlots(worldLocationId);
        int index = configured.indexOf(sourceMobSlotId);
        if (index < 0) return new AdjacentStages(null, null);
        Integer previous = index > 0 ? MobZoneIds.encode(worldLocationId, configured.get(index - 1)) : null;
        Integer next = index + 1 < configured.size()
                ? MobZoneIds.encode(worldLocationId, configured.get(index + 1))
                : null;
        return new AdjacentStages(previous, next);
    }

    /** Forward points toward next stage. On the final stage it points directly away from previous. */
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
        if (trigger == null) {
            throw new IllegalStateException(
                    "Transition trigger is not configured for Location " + worldLocationId
                            + ", mobSlot " + sourceMobSlotId);
        }
        return trigger;
    }

    private List<StageTransitionTriggerDefinition> orderedTriggers() {
        List<StageTransitionTriggerDefinition> ordered = new ArrayList<>(triggers.values());
        ordered.sort(Comparator
                .comparingInt(StageTransitionTriggerDefinition::getWorldLocationId)
                .thenComparingInt(StageTransitionTriggerDefinition::getSourceMobSlotId));
        return ordered;
    }

    private void sendConfiguredMessage(EntityPlayerMP player, String message, DisplayMode mode) {
        if (message == null || message.trim().isEmpty()) return;
        TextComponentString component = new TextComponentString(message);
        if (mode == DisplayMode.CHAT) player.sendMessage(component);
        else player.sendStatusMessage(component, true);
    }

    private String key(int worldLocationId, int sourceMobSlotId) {
        return worldLocationId + ":" + sourceMobSlotId;
    }

    private static final class AdjacentStages {
        final Integer previousId;
        final Integer nextId;

        AdjacentStages(Integer previousId, Integer nextId) {
            this.previousId = previousId;
            this.nextId = nextId;
        }
    }
}
