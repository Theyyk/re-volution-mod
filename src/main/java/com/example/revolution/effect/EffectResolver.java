package com.example.revolution.effect;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Shared stat/effect resolution layer for Rune/Prestige/Pet/Artifact-style sources. */
public final class EffectResolver {
    public enum Stat { CLICK_DAMAGE_MULTIPLIER }

    public interface Source {
        List<Contribution> contributions(UUID playerUuid, Stat stat);
    }

    public static final class Contribution {
        public final Stat stat;
        public final BigDecimal additiveMultiplier;
        public final String origin;

        public Contribution(Stat stat, BigDecimal additiveMultiplier, String origin) {
            this.stat = stat;
            this.additiveMultiplier = additiveMultiplier == null ? BigDecimal.ZERO : additiveMultiplier;
            this.origin = origin == null ? "unknown" : origin;
        }
    }

    private final List<Source> sources = new ArrayList<>();

    public void register(Source source) { if (source != null && !sources.contains(source)) sources.add(source); }

    public BigDecimal multiplier(UUID playerUuid, Stat stat) {
        BigDecimal result = BigDecimal.ONE;
        for (Source source : new ArrayList<>(sources)) {
            List<Contribution> contributions = source.contributions(playerUuid, stat);
            if (contributions == null) continue;
            for (Contribution contribution : contributions) {
                if (contribution != null && contribution.stat == stat) result = result.add(contribution.additiveMultiplier);
            }
        }
        return result.max(BigDecimal.ZERO);
    }

    public List<Contribution> inspect(UUID playerUuid, Stat stat) {
        List<Contribution> result = new ArrayList<>();
        for (Source source : new ArrayList<>(sources)) {
            List<Contribution> contributions = source.contributions(playerUuid, stat);
            if (contributions != null) result.addAll(contributions);
        }
        return Collections.unmodifiableList(result);
    }
}
