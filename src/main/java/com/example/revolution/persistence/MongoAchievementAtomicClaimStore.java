package com.example.revolution.persistence;

import com.example.revolution.achievement.AchievementAtomicClaimStore;
import com.example.revolution.achievement.AchievementPlayerState;
import com.example.revolution.achievement.AchievementStage;
import com.mongodb.client.result.UpdateResult;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.Decimal128;

/**
 * Production claim transaction. Rewards and Achievement state live in one players document, so a
 * single aggregation-pipeline update closes the crash window between reward grant and claim save.
 */
public final class MongoAchievementAtomicClaimStore implements AchievementAtomicClaimStore {
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoAchievementAtomicClaimStore(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
    }

    @Override public boolean available() { return enabled; }

    @Override
    public boolean commit(UUID playerUuid, String achievementId, int expectedPriorClaimedStage,
                          AchievementStage stage, AchievementPlayerState nextState) {
        if (!enabled || playerUuid == null || achievementId == null || stage == null || nextState == null) return false;
        if (!connection.isConnected()) return false;
        String claimKey = achievementId + ":" + stage.stageIndex;
        String claimedPath = "$achievementState.claimedStages." + achievementId;

        Document filter = new Document("_id", playerUuid.toString())
                .append("achievementClaimReceipts", new Document("$ne", claimKey))
                .append("$expr", new Document("$eq", Arrays.asList(
                        new Document("$ifNull", Arrays.asList(claimedPath, 0)), expectedPriorClaimedStage)));

        Document set = new Document("crystals", addExact("$crystals", stage.crystals))
                .append("resources.aura_key", addExact("$resources.aura_key", stage.auraKeys))
                .append("godWorshipPersistent.offerings", addExact("$godWorshipPersistent.offerings", stage.offerings))
                .append("achievementState", new Document("$literal", MongoAchievementRepository.encode(nextState)))
                .append("achievementClaimReceipts", new Document("$setUnion", Arrays.asList(
                        new Document("$ifNull", Arrays.asList("$achievementClaimReceipts", Collections.emptyList())),
                        Collections.singletonList(claimKey))));

        List<Document> pipeline = Collections.singletonList(new Document("$set", set));
        UpdateResult result = connection.collection("players").updateOne(filter, pipeline);
        return result.getMatchedCount() == 1L && result.getModifiedCount() == 1L;
    }

    private static Document addExact(String field, BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount.max(BigDecimal.ZERO);
        Document existing = new Document("$toDecimal", new Document("$ifNull", Arrays.asList(field, "0")));
        return new Document("$toString", new Document("$add", Arrays.asList(existing, new Decimal128(safe))));
    }
}
