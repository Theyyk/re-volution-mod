package com.example.revolution.resource.service;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayEventSink;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.resource.repository.ResourceRepository;
import java.math.BigDecimal;
import java.util.UUID;
import static com.example.revolution.resource.definition.ResourceDefinitions.*;

/** Validates resource actions and preserves the existing read/modify/write economy. */
public final class ResourceService {
    public interface LateRewardMultiplier {
        BigDecimal multiplier(UUID playerUuid, String canonicalResource);
    }

    public static final LateRewardMultiplier IDENTITY_LATE_REWARD = (playerUuid, canonicalResource) -> BigDecimal.ONE;

    private final ResourceRepository repository;
    private final EffectResolver effects;
    private GameplayEventSink gameplayEvents = GameplayEventSink.NONE;
    private LateRewardMultiplier lateRewardMultiplier = IDENTITY_LATE_REWARD;

    public ResourceService(ResourceRepository repository) { this(repository, null); }

    public ResourceService(ResourceRepository repository, EffectResolver effects) {
        this.repository = repository;
        this.effects = effects;
    }

    public void setGameplayEventSink(GameplayEventSink gameplayEvents) {
        this.gameplayEvents = gameplayEvents == null ? GameplayEventSink.NONE : gameplayEvents;
    }

    public void setLateRewardMultiplier(LateRewardMultiplier lateRewardMultiplier) {
        this.lateRewardMultiplier = lateRewardMultiplier == null ? IDENTITY_LATE_REWARD : lateRewardMultiplier;
    }

    public int getPlayerResource(UUID uuid, String resource) {
        BigDecimal value = repository.getPlayerResourceValue(uuid, resource);
        if (value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) >= 0) return Integer.MAX_VALUE;
        return Math.max(0, value.intValue());
    }

    public long getPlayerResourceLong(UUID uuid, String resource) {
        BigDecimal value = repository.getPlayerResourceValue(uuid, resource);
        if (value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) >= 0) return Long.MAX_VALUE;
        return Math.max(0L, value.longValue());
    }

    public synchronized boolean grantPlayerResource(UUID uuid, String resource, BigDecimal amount) {
        String canonical = findCanonicalName(resource);
        if (canonical == null || uuid == null || amount == null || amount.signum() < 0) return false;
        if (amount.signum() == 0) return true;
        BigDecimal next = LargeNumbers.normalize(repository.getPlayerResourceValue(uuid, canonical).add(amount));
        repository.writePlayerResource(uuid, canonical, next);
        return true;
    }

    /**
     * Grants an authoritative earned reward. Normal effects are resolved first, then the dedicated
     * late reward layer (Aura in v2.9), then observers receive exactly the persisted amount.
     */
    public synchronized boolean grantEarnedPlayerResource(UUID uuid, String resource, BigDecimal baseAmount) {
        String canonical = findCanonicalName(resource);
        if (canonical == null || uuid == null || baseAmount == null || baseAmount.signum() < 0) return false;
        if (baseAmount.signum() == 0) return true;
        BigDecimal normal = rewardMultiplier(uuid, canonical);
        BigDecimal late = lateRewardMultiplier.multiplier(uuid, canonical);
        if (late == null || late.signum() < 0) late = BigDecimal.ONE;
        BigDecimal granted = LargeNumbers.normalize(baseAmount.multiply(normal).multiply(late));
        if (granted.signum() <= 0) return true;
        BigDecimal next = LargeNumbers.normalize(repository.getPlayerResourceValue(uuid, canonical).add(granted));
        repository.writePlayerResource(uuid, canonical, next);
        gameplayEvents.earnedResource(uuid, canonical, granted);
        return true;
    }

    public boolean grantEarnedPlayerResource(UUID uuid, String resource, int amount) {
        return amount >= 0 && grantEarnedPlayerResource(uuid, resource, BigDecimal.valueOf(amount));
    }

    private BigDecimal rewardMultiplier(UUID uuid, String canonical) {
        if (effects == null) return BigDecimal.ONE;
        String normalized = normalize(canonical);
        if ("coins".equals(normalized)) return effects.multiplier(uuid, EffectResolver.Stat.COIN_REWARD_MULTIPLIER);
        if (RUNE_DUST.equals(normalized) || RUNE_SHARDS.equals(normalized) || RUNE_CORES.equals(normalized)) {
            return effects.multiplier(uuid, EffectResolver.Stat.RUNE_RESOURCE_REWARD_MULTIPLIER);
        }
        return BigDecimal.ONE;
    }

    public boolean grantPlayerResource(UUID uuid, String resource, int amount) {
        return grantPlayerResource(uuid, resource, BigDecimal.valueOf(amount));
    }

    public synchronized boolean spendPlayerResource(UUID uuid, String resource, BigDecimal amount) {
        if (uuid == null || amount == null || amount.signum() < 0) return false;
        String canonical = findCanonicalName(resource);
        if (canonical == null) return false;
        BigDecimal current = repository.getPlayerResourceValue(uuid, canonical);
        if (current.compareTo(amount) < 0) return false;
        repository.writePlayerResource(uuid, canonical, LargeNumbers.normalize(current.subtract(amount)));
        return true;
    }

    public boolean spendPlayerResource(UUID uuid, String resource, int amount) {
        return amount >= 0 && spendPlayerResource(uuid, resource, BigDecimal.valueOf(amount));
    }

    public boolean canAfford(UUID uuid, String resource, BigDecimal amount) {
        return uuid != null && amount != null && amount.signum() >= 0
                && repository.getPlayerResourceValue(uuid, resource).compareTo(amount) >= 0;
    }

    public boolean canAfford(UUID uuid, String resource, int amount) {
        return amount >= 0 && canAfford(uuid, resource, BigDecimal.valueOf(amount));
    }

    /** Positive deltas are gameplay earnings; negative deltas remain ordinary spending. */
    public int addPlayerResource(UUID uuid, String resource, int amount) {
        if (amount > 0) grantEarnedPlayerResource(uuid, resource, amount);
        else if (amount < 0) spendPlayerResource(uuid, resource, (int) Math.min(Integer.MAX_VALUE, -(long) amount));
        return getPlayerResource(uuid, resource);
    }

    public int setPlayerResource(UUID uuid, String resource, int value) {
        String canonical = findCanonicalName(resource);
        if (canonical == null) return 0;
        setPlayerResourceValue(uuid, canonical, BigDecimal.valueOf(Math.max(0, value)));
        return getPlayerResource(uuid, canonical);
    }

    public synchronized boolean setPlayerResourceValue(UUID uuid, String resource, BigDecimal value) {
        String canonical = findCanonicalName(resource);
        if (canonical == null || uuid == null || value == null || value.signum() < 0) return false;
        repository.writePlayerResource(uuid, canonical, LargeNumbers.normalize(value));
        return true;
    }

    public synchronized boolean addPlayerResourceValue(UUID uuid, String resource, BigDecimal amount) {
        String canonical = findCanonicalName(resource);
        if (canonical == null || uuid == null || amount == null) return false;
        BigDecimal next = repository.getPlayerResourceValue(uuid, canonical).add(amount);
        if (next.signum() < 0) return false;
        repository.writePlayerResource(uuid, canonical, LargeNumbers.normalize(next));
        return true;
    }

    public void purgeDeprecatedLightnings(){ repository.purgeDeprecatedLightnings(); }
    public void purgeDeprecatedLightnings(UUID uuid){ repository.purgeDeprecatedLightnings(uuid); }
    public BigDecimal getPlayerResourceValue(UUID uuid, String resource){ return repository.getPlayerResourceValue(uuid, resource); }
}
