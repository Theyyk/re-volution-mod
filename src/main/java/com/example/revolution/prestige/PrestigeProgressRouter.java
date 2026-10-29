package com.example.revolution.prestige;

import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.gameplay.GameplayEventSink;
import java.math.BigDecimal;
import java.util.UUID;

/** Routes confirmed gameplay facts through branch definitions; contains no combat or reward calculation. */
public final class PrestigeProgressRouter implements GameplayEventSink {
    private final PrestigeService prestige;
    private final PrestigeBranchRegistry registry;

    public PrestigeProgressRouter(PrestigeService prestige, PrestigeBranchRegistry registry) {
        this.prestige = prestige;
        this.registry = registry;
    }

    @Override
    public void confirmedDamage(UUID playerUuid, GameplayDamageSource source, BigDecimal amount) {
        if (playerUuid == null || source == null || amount == null || amount.signum() <= 0) return;
        for (PrestigeBranch branch : registry.branches()) {
            BigDecimal progress = registry.get(branch).progressSource.fromDamage(source, amount);
            if (progress.signum() > 0) prestige.recordProgress(playerUuid, branch, progress);
        }
    }

    @Override
    public void earnedResource(UUID playerUuid, String resource, BigDecimal amount) {
        if (playerUuid == null || resource == null || amount == null || amount.signum() <= 0) return;
        for (PrestigeBranch branch : registry.branches()) {
            BigDecimal progress = registry.get(branch).progressSource.fromResource(resource, amount);
            if (progress.signum() > 0) prestige.recordProgress(playerUuid, branch, progress);
        }
    }
}
