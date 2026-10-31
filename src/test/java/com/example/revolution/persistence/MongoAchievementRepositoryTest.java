package com.example.revolution.persistence;

import com.example.revolution.achievement.AchievementPlayerState;
import java.math.BigDecimal;
import java.util.Arrays;
import org.bson.Document;

/** Pure codec regression for v2.9 compatibility and permanent v2.10 Achievement state. */
public final class MongoAchievementRepositoryTest {
    public static void main(String[] args) {
        AchievementPlayerState migrated = MongoAchievementRepository.migrateFromV29(new Document());
        check(migrated.achievementPoints == 0L, "v2.9 missing state starts with zero points");
        check(migrated.achievementProgress.isEmpty(), "v2.9 without recoverable facts does not invent progress");
        check(migrated.highestClaimedStage.isEmpty(), "v2.9 missing state does not invent claims");
        check(migrated.everSelectedMasteryIds.isEmpty(), "v2.9 missing state does not invent mastery history");
        check(migrated.everObtainedPetIds.isEmpty(), "v2.9 missing state does not invent pet history");
        check(migrated.visitedLocationIds.isEmpty(), "v2.9 missing state does not invent location history");

        Document v29WithRecoverableHistory = new Document("auraState",
                new Document("totalChestAuraDrops", 137L).append("totalChestOpens", 137L))
                .append("petSystem", new Document("pets", new Document("pet_persistent", new Document("rank", "ADVANCED")))
                        .append("run", new Document("purchasedPetIds", Arrays.asList("pet_current", "pet_persistent"))))
                .append("prestige", new Document("activeRun", new Document("branch", "CLICK"))
                        .append("branches", new Document("POISON", new Document("bestSavedProgress", "25")
                                .append("rankMetaLevels", new Document()).append("masteryTier", 0))
                                .append("BURNING", new Document("bestSavedProgress", "0")
                                        .append("rankMetaLevels", new Document("rank_1", 1)).append("masteryTier", 0))
                                .append("CURSE", new Document("bestSavedProgress", "0")
                                        .append("rankMetaLevels", new Document()).append("masteryTier", 0))));
        AchievementPlayerState recovered = MongoAchievementRepository.migrateFromV29(v29WithRecoverableHistory);
        check(recovered.progress("aura_drops").compareTo(new BigDecimal("137")) == 0,
                "exact v2.9 Aura chest-drop statistic is bootstrapped");
        check(recovered.everObtainedPetIds.size() == 2
                        && recovered.everObtainedPetIds.contains("pet_persistent")
                        && recovered.everObtainedPetIds.contains("pet_current"),
                "currently factual v2.9 Pet ownership is deduplicated and recovered");
        check(recovered.progress("pets_obtained").compareTo(new BigDecimal("2")) == 0,
                "Pet migration progress equals recovered lifetime lower bound");
        check(recovered.everSelectedMasteryIds.size() == 3
                        && recovered.everSelectedMasteryIds.contains("CLICK")
                        && recovered.everSelectedMasteryIds.contains("POISON")
                        && recovered.everSelectedMasteryIds.contains("BURNING"),
                "active/saved/meta-confirmed Mastery branches are recovered");
        check(!recovered.everSelectedMasteryIds.contains("CURSE"),
                "empty untouched branch state is not inferred as historical selection");
        check(recovered.progress("mastery_branches").compareTo(new BigDecimal("3")) == 0,
                "Mastery migration progress equals recovered lifetime lower bound");
        check(recovered.visitedLocationIds.isEmpty(), "location history is not guessed from unrelated progression");

        AchievementPlayerState source = new AchievementPlayerState();
        source.achievementProgress.put("damage_total", new BigDecimal("12345678901234567890.25"));
        source.highestClaimedStage.put("damage_total", 3);
        source.everSelectedMasteryIds.add("CLICK");
        source.everObtainedPetIds.add("pet_18");
        source.visitedLocationIds.add("10041");
        source.revealedHiddenAchievements.add("aura_mythic");
        source.achievementPoints = 987654321L;

        Document encoded = MongoAchievementRepository.encode(source);
        AchievementPlayerState restored = MongoAchievementRepository.decode(encoded);
        check(restored.progress("damage_total").compareTo(new BigDecimal("12345678901234567890.25")) == 0,
                "exact large progress survives codec");
        check(restored.claimedStage("damage_total") == 3, "claimed stage survives codec");
        check(restored.everSelectedMasteryIds.contains("CLICK"), "mastery history survives codec");
        check(restored.everObtainedPetIds.contains("pet_18"), "pet history survives codec");
        check(restored.visitedLocationIds.contains("10041"), "location history survives codec");
        check(restored.revealedHiddenAchievements.contains("aura_mythic"), "hidden reveal survives codec");
        check(restored.achievementPoints == 987654321L, "Achievement Points survive codec");
        check(encoded.getInteger("schemaVersion") == 1, "explicit Achievement schema version is persisted");
        System.out.println("Achievement Mongo codec and v2.9 factual migration regressions passed");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
