package com.example.revolution.mechanic;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Set;

/** v2.12.0 canonical identities and permanent player state for mechanic unlocks. */
public final class MechanicUnlockModel {
    private MechanicUnlockModel() {}

    public enum Rarity { COMMON, IMPROVED, ADVANCED, MYTHIC, ANCIENT }
    public enum RequirementType { RUNE_PROGRESS_MAX }
    public enum Visibility { VISIBLE_WHEN_LOCKED, HIDDEN_UNTIL_PARENT_UNLOCKED }

    public static final class Definition {
        public final String unlockId;
        public final boolean enabled;
        public final String targetMechanic;
        public final String parentUnlockId;
        public final Visibility visibility;
        public final RequirementType requirementType;
        public final Rarity rarity;
        public final int target;
        public final int order;
        public final String displayName;

        public Definition(String unlockId, boolean enabled, String targetMechanic, String parentUnlockId,
                          Visibility visibility, RequirementType requirementType, Rarity rarity,
                          int target, int order, String displayName) {
            this.unlockId = unlockId;
            this.enabled = enabled;
            this.targetMechanic = targetMechanic;
            this.parentUnlockId = parentUnlockId == null ? "" : parentUnlockId;
            this.visibility = visibility;
            this.requirementType = requirementType;
            this.rarity = rarity;
            this.target = Math.max(0, target);
            this.order = order;
            this.displayName = displayName == null ? unlockId : displayName;
        }
    }

    public static final class PlayerState {
        public final EnumMap<Rarity, Integer> maxRuneProgressByRarity = new EnumMap<>(Rarity.class);
        public final Set<String> unlockedMechanicIds = new LinkedHashSet<>();
        public final Set<String> forceUnlockedMechanicIds = new LinkedHashSet<>();
        public final Set<String> forceLockedMechanicIds = new LinkedHashSet<>();

        public PlayerState() {
            for (Rarity rarity : Rarity.values()) maxRuneProgressByRarity.put(rarity, 0);
        }

        public int maxProgress(Rarity rarity) {
            Integer value = maxRuneProgressByRarity.get(rarity);
            return value == null ? 0 : Math.max(0, value);
        }

        public PlayerState copy() {
            PlayerState copy = new PlayerState();
            copy.maxRuneProgressByRarity.putAll(maxRuneProgressByRarity);
            copy.unlockedMechanicIds.addAll(unlockedMechanicIds);
            copy.forceUnlockedMechanicIds.addAll(forceUnlockedMechanicIds);
            copy.forceLockedMechanicIds.addAll(forceLockedMechanicIds);
            return copy;
        }
    }
}
