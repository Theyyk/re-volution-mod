package com.example.revolution.resource.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** Persistence port. Implementations preserve the existing document contract. */
public interface ResourceRepository {
    void purgeDeprecatedLightnings();
    void purgeDeprecatedLightnings(UUID uuid);
    BigDecimal getPlayerResourceValue(UUID uuid, String resource);
    void writePlayerResource(UUID uuid, String canonical, BigDecimal value);
}
