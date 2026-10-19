package com.example.revolution.rune.definition;

/**
 * Reference-style "+N to the power of <specific rune/family>" bonus.
 * The target is deliberately separate from RuneStat's scalar value so a
 * targeted power bonus cannot accidentally become a global channel bonus.
 */
public final class RuneTargetedPower {
    public final String targetKey;
    public final String targetDisplayName;
    public final double flatBonus;

    public RuneTargetedPower(String targetKey, String targetDisplayName, double flatBonus) {
        if (targetKey == null || targetKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Target key is required");
        }
        if (targetDisplayName == null || targetDisplayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Target display name is required");
        }
        if (!Double.isFinite(flatBonus)) {
            throw new IllegalArgumentException("Targeted power bonus must be finite");
        }
        this.targetKey = targetKey.trim();
        this.targetDisplayName = targetDisplayName.trim();
        this.flatBonus = flatBonus;
    }

    public RuneStatType type() {
        return RuneStatType.RUNE_POWER_FLAT;
    }

    public double resolve(int rank, double enhancementMultiplier) {
        return RuneStat.scaled(RuneStatType.RUNE_POWER_FLAT, flatBonus)
                .resolve(rank, enhancementMultiplier)
                .finalValue;
    }

    public String tooltipLine(int rank, double enhancementMultiplier) {
        return RuneStatType.RUNE_POWER_FLAT.format(resolve(rank, enhancementMultiplier))
                + " к силе " + targetDisplayName;
    }
}
