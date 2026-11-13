package com.example.revolution.location.service;

import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.mob.service.MobProgressionService;
import com.example.revolution.prestige.PrestigeService;
import com.example.revolution.realm.definition.RealmCatalog;
import com.example.revolution.realm.definition.RealmDefinition;
import com.example.revolution.realm.service.RealmService;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import org.apache.logging.log4j.Logger;

/** Retryable post-commit return; this operation never replays Reset, charge, or Tier changes. */
public final class LocationResetReturnService {
    private final PrestigeService prestige;
    private final MobProgressionService progression;
    private final StageTransitionService transitions;
    private final RealmService realms;
    private final Logger logger;

    public LocationResetReturnService(PrestigeService prestige, MobProgressionService progression,
                                      StageTransitionService transitions, RealmService realms, Logger logger) {
        this.prestige = prestige;
        this.progression = progression;
        this.transitions = transitions;
        this.realms = realms;
        this.logger = logger;
    }

    /** Temporary destination policy: the first configured slot in the first configured Location. */
    public int startingStageId() {
        Integer location = progression.firstConfiguredWorldLocationId();
        if (location == null) throw new IllegalStateException("No starting Location is configured");
        List<Integer> slots = progression.configuredSlots(location);
        if (slots.isEmpty()) throw new IllegalStateException("No starting stage is configured");
        return MobZoneIds.encode(location, slots.get(0));
    }

    public boolean tryReturn(EntityPlayerMP player) {
        if (player == null || player.getServer() == null) return false;
        String pendingRunId = prestige.pendingLocationReturn(player.getUniqueID());
        if (pendingRunId == null) return true;
        try {
            int destination = startingStageId();
            // RealmService alone owns dimensional transfers. StageTransitionService owns
            // final-point chunk loading, collision/support validation, and teleportation.
            if (!RealmCatalog.isRealmDimension(player.dimension)) {
                MinecraftServer server = player.getServer();
                RealmDefinition realm = RealmCatalog.leastPopulated(realms.population(server, player.getUniqueID()));
                realms.join(server, player, realm.getRealmId());
            }
            if (!transitions.isAtConfiguredStage(player, destination)) transitions.moveToStage(player, destination);
            if (!prestige.completeLocationReturn(player.getUniqueID(), pendingRunId))
                throw new IllegalStateException("Starting stage reached but return acknowledgement is pending");
            return true;
        } catch (RuntimeException problem) {
            if (logger != null) logger.warn("Location return pending for player {} run {}: {}",
                    player.getUniqueID(), pendingRunId, problem.toString());
            player.sendStatusMessage(new TextComponentString("Возврат в стартовую зону ожидает восстановления сервера"), true);
            return false;
        }
    }
}
