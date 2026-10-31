package com.example.revolution.achievement;

import com.example.revolution.god.GodWorshipModel;
import com.example.revolution.god.GodWorshipRepository;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import java.math.BigDecimal;
import java.util.UUID;

/** Uses existing Crystals/Aura Key resources and the persistent God Worship Offerings wallet. */
public final class CanonicalAchievementRewardGateway implements AchievementRewardGateway {
    private final ResourceService resources;
    private final GodWorshipRepository gods;

    public CanonicalAchievementRewardGateway(ResourceService resources, GodWorshipRepository gods) {
        if (resources == null || gods == null) throw new IllegalArgumentException("reward wallets are required");
        this.resources = resources;
        this.gods = gods;
    }

    @Override
    public synchronized boolean grantStageRewards(UUID playerUuid, String claimKey, AchievementStage stage) {
        if (playerUuid == null || claimKey == null || claimKey.trim().isEmpty() || stage == null) return false;
        boolean crystalsGranted = grant(resources, playerUuid, "crystals", stage.crystals);
        if (!crystalsGranted) return false;
        boolean keysGranted = grant(resources, playerUuid, ResourceDefinitions.AURA_KEY, stage.auraKeys);
        if (!keysGranted) {
            rollback(resources, playerUuid, "crystals", stage.crystals);
            return false;
        }
        try {
            if (stage.offerings.signum() > 0) {
                GodWorshipModel.PersistentState persistent = gods.loadPersistentState(playerUuid);
                BigDecimal before = persistent.offerings == null ? BigDecimal.ZERO : persistent.offerings.max(BigDecimal.ZERO);
                persistent.offerings = before.add(stage.offerings);
                gods.savePersistentState(playerUuid, persistent);
            }
            return true;
        } catch (RuntimeException failure) {
            rollback(resources, playerUuid, ResourceDefinitions.AURA_KEY, stage.auraKeys);
            rollback(resources, playerUuid, "crystals", stage.crystals);
            return false;
        }
    }

    private static boolean grant(ResourceService resources, UUID playerUuid, String resource, BigDecimal amount) {
        return amount.signum() == 0 || resources.grantPlayerResource(playerUuid, resource, amount);
    }

    private static void rollback(ResourceService resources, UUID playerUuid, String resource, BigDecimal amount) {
        if (amount.signum() > 0) resources.spendPlayerResource(playerUuid, resource, amount);
    }
}
