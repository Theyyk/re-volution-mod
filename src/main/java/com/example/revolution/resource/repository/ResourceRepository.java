package com.example.revolution.resource.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** Persistence port. Implementations preserve the existing document contract. */
public interface ResourceRepository {
    void purgeDeprecatedLightnings();
    void purgeDeprecatedLightnings(UUID uuid);
    BigDecimal getPlayerResourceValue(UUID uuid, String resource);
    void writePlayerResource(UUID uuid, String canonical, BigDecimal value);

    /** One observed balance and reset generation, used to avoid stale read/modify/write. */
    final class ResourceSnapshot {
        public final BigDecimal value;
        public final Object observedValue;
        public final String resetEpoch;
        public ResourceSnapshot(BigDecimal value, Object observedValue, String resetEpoch) {
            this.value = value;
            this.observedValue = observedValue;
            this.resetEpoch = resetEpoch;
        }
    }

    /** Compatibility defaults allow existing in-memory test repositories to keep their contract. */
    default ResourceSnapshot resourceSnapshot(UUID uuid, String canonical) {
        return new ResourceSnapshot(getPlayerResourceValue(uuid, canonical), null, null);
    }

    default boolean compareAndSetPlayerResource(UUID uuid, String canonical,
                                                ResourceSnapshot observed, BigDecimal next) {
        writePlayerResource(uuid, canonical, next);
        return true;
    }

    /** Run-scoped mutations must not spill into a new run after a reset. */
    default boolean compareAndSetPlayerResourceForRun(UUID uuid, String canonical,
                                                      ResourceSnapshot observed, BigDecimal next,
                                                      String expectedRunId) {
        return compareAndSetPlayerResource(uuid, canonical, observed, next);
    }
}
