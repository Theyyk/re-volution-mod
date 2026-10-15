package com.example.customguimod;

/**
 * Reference-style "+N to the power of <specific rune/family>" bonus.
 * The target is deliberately separate from RuneStat's scalar value so a
 * targeted power bonus cannot accidentally become a global channel bonus.
 */
final class RuneTargetedPower {
    final String targetKey;
    final String targetDisplayName;
    final double flatBonus;

    RuneTargetedPower(String targetKey, String targetDisplayName, double flatBonus) {
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

    RuneStatType type() {
        return RuneStatType.CARD_POWER_FLAT;
    }

    String tooltipLine() {
        return RuneStatType.CARD_POWER_FLAT.format(flatBonus) + " к силе рун: " + targetDisplayName;
    }
}
