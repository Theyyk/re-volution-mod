package com.example.revolution.location.service;

import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition.DisplayMode;
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

/** Runtime for independent transition trigger zones, messages, and per-player auto preference. */
public final class StageTransitionTriggerService {
    private static final long TELEPORT_COOLDOWN_TICKS = 10L;

    private final StageTransitionTriggerRepository repository;
    private final StageTransitionPreferenceRepository preferences;
    private final MobProgressionService progression;
    private final StageTransitionService transitions;
    private final Map<String, StageTransitionTriggerDefinition> triggers = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> insideByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldownUntilTick = new ConcurrentHashMap<>();

    public StageTransitionTriggerService(
            StageTransitionTriggerRepository repository,
            StageTransitionPreferenceRepository preferences,
            MobProgressionService progression,
            StageTransitionService transitions) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        if (preferences == null) throw new IllegalArgumentException("preferences is required");
        if (progression == null) throw new IllegalArgumentException("progression is required");
        if (transitions == null) throw new IllegalArgumentException("transitions is required");
        this.repository = repository;
        this.preferences = preferences;
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

    public StageTransitionTriggerDefinition configureTrigger(
            int worldLocationId,
            int mobSlotId,
            double x,
            double y,
            double z,
            double radius,
            double halfHeight) {
        int targetId = MobZoneIds.encode(worldLocationId, mobSlotId);
        if (!MobZoneIds.isMobZoneId(targetId)) {
            throw new IllegalArgumentException("Invalid stage target");
        }
        StageTransitionTriggerDefinition previous = triggers.get(key(worldLocationId, mobSlotId));
        StageTransitionTriggerDefinition configured = new StageTransitionTriggerDefinition(
                worldLocationId,
                mobSlotId,
                x,
                y,
                z,
                radius,
                halfHeight,
                previous == null ? DisplayMode.ACTION_BAR : previous.getDisplayMode(),
                previous == null ? null : previous.getLockedMessage(),
                previous == null ? null : previous.getEnteringMessage());
        repository.upsert(configured);
        triggers.put(configured.key(), configured);
        return configured;
    }

    public void clearTrigger(int worldLocationId, int mobSlotId) {
        repository.delete(worldLocationId, mobSlotId);
        triggers.remove(key(worldLocationId, mobSlotId));
        String removedKey = key(worldLocationId, mobSlotId);
        for (Set<String> inside : insideByPlayer.values()) inside.remove(removedKey);
    }

    public StageTransitionTriggerDefinition setMessage(
            int worldLocationId, int mobSlotId, boolean locked, String message) {
        StageTransitionTriggerDefinition trigger = requireTrigger(worldLocationId, mobSlotId);
        StageTransitionTriggerDefinition updated = locked
                ? trigger.withMessages(message, trigger.getEnteringMessage())
                : trigger.withMessages(trigger.getLockedMessage(), message);
        repository.upsert(updated);
        triggers.put(updated.key(), updated);
        return updated;
    }

    public StageTransitionTriggerDefinition clearMessage(int worldLocationId, int mobSlotId, boolean locked) {
        return setMessage(worldLocationId, mobSlotId, locked, null);
    }

    public StageTransitionTriggerDefinition setDisplayMode(
            int worldLocationId, int mobSlotId, DisplayMode mode) {
        StageTransitionTriggerDefinition trigger = requireTrigger(worldLocationId, mobSlotId);
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
            if (!trigger.contains(player.posX, player.posY, player.posZ)) continue;
            nowInside.add(trigger.key());
            if (!previousInside.contains(trigger.key())) entered.add(trigger);
        }
        insideByPlayer.put(playerId, nowInside);
        if (entered.isEmpty()) return;

        long now = player.world.getTotalWorldTime();
        Long cooldown = cooldownUntilTick.get(playerId);
        if (cooldown != null && now < cooldown) return;

        for (StageTransitionTriggerDefinition trigger : entered) {
            int targetId = MobZoneIds.encode(trigger.getWorldLocationId(), trigger.getMobSlotId());
            if (!progression.isUnlocked(playerId, targetId)) {
                sendConfiguredMessage(player, trigger.getLockedMessage(), trigger.getDisplayMode());
                continue;
            }
            if (!preferences.isAutoTransitionEnabled(playerId)) continue;
            try {
                transitions.moveToStage(player, targetId);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                continue;
            }
            sendConfiguredMessage(player, trigger.getEnteringMessage(), trigger.getDisplayMode());
            cooldownUntilTick.put(playerId, now + TELEPORT_COOLDOWN_TICKS);
            break;
        }
    }

    public void forgetPlayer(UUID playerUuid) {
        if (playerUuid == null) return;
        insideByPlayer.remove(playerUuid);
        cooldownUntilTick.remove(playerUuid);
    }

    private StageTransitionTriggerDefinition requireTrigger(int worldLocationId, int mobSlotId) {
        StageTransitionTriggerDefinition trigger = triggers.get(key(worldLocationId, mobSlotId));
        if (trigger == null) {
            throw new IllegalStateException(
                    "Transition trigger is not configured for Location " + worldLocationId + ", mobSlot " + mobSlotId);
        }
        return trigger;
    }

    private List<StageTransitionTriggerDefinition> orderedTriggers() {
        List<StageTransitionTriggerDefinition> ordered = new ArrayList<>(triggers.values());
        ordered.sort(Comparator
                .comparingInt(StageTransitionTriggerDefinition::getWorldLocationId)
                .thenComparingInt(StageTransitionTriggerDefinition::getMobSlotId));
        return ordered;
    }

    private void sendConfiguredMessage(EntityPlayerMP player, String message, DisplayMode mode) {
        if (message == null || message.trim().isEmpty()) return;
        TextComponentString component = new TextComponentString(message);
        if (mode == DisplayMode.CHAT) player.sendMessage(component);
        else player.sendStatusMessage(component, true);
    }

    private String key(int worldLocationId, int mobSlotId) {
        return worldLocationId + ":" + mobSlotId;
    }
}
