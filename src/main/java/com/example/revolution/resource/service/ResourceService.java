package com.example.revolution.resource.service;

import com.example.revolution.number.LargeNumbers;
import com.example.revolution.resource.repository.ResourceRepository;
import java.math.BigDecimal;
import java.util.UUID;
import static com.example.revolution.resource.definition.ResourceDefinitions.*;

/** Validates resource actions and preserves the existing read/modify/write economy. */
public final class ResourceService {
    private final ResourceRepository repository;
    public ResourceService(ResourceRepository repository) { this.repository = repository; }

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
        if (canonical == null || amount == null || amount.signum() < 0) return false;
        if (amount.signum() == 0) return true;
        BigDecimal next = LargeNumbers.normalize(repository.getPlayerResourceValue(uuid, canonical).add(amount));
        repository.writePlayerResource(uuid, canonical, next);
        return true;
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

    public int addPlayerResource(UUID uuid, String resource, int amount) {
        if (amount > 0) grantPlayerResource(uuid, resource, amount);
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
