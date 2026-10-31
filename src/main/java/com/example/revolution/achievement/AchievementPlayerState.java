package com.example.revolution.achievement;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Permanent state. Mastery/reset-scoped systems must never mutate this object. */
public final class AchievementPlayerState {
    public final Map<String, BigDecimal> achievementProgress = new LinkedHashMap<>();
    public final Map<String, Integer> highestClaimedStage = new LinkedHashMap<>();
    public final Set<String> everSelectedMasteryIds = new LinkedHashSet<>();
    public final Set<String> everObtainedPetIds = new LinkedHashSet<>();
    public final Set<String> visitedLocationIds = new LinkedHashSet<>();
    public final Set<String> revealedHiddenAchievements = new LinkedHashSet<>();
    public long achievementPoints;

    public BigDecimal progress(String achievementId) {
        BigDecimal value = achievementProgress.get(achievementId);
        return value == null ? BigDecimal.ZERO : value;
    }

    public int claimedStage(String achievementId) {
        Integer value = highestClaimedStage.get(achievementId);
        return value == null ? 0 : Math.max(0, value);
    }

    public AchievementPlayerState copy() {
        AchievementPlayerState copy = new AchievementPlayerState();
        copy.achievementProgress.putAll(achievementProgress);
        copy.highestClaimedStage.putAll(highestClaimedStage);
        copy.everSelectedMasteryIds.addAll(everSelectedMasteryIds);
        copy.everObtainedPetIds.addAll(everObtainedPetIds);
        copy.visitedLocationIds.addAll(visitedLocationIds);
        copy.revealedHiddenAchievements.addAll(revealedHiddenAchievements);
        copy.achievementPoints = Math.max(0L, achievementPoints);
        return copy;
    }
}
