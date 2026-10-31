package com.example.revolution.persistence;

import com.example.revolution.gameplay.GameplayEventType;
import com.example.revolution.quest.definition.QuestState;
import com.example.revolution.quest.repository.QuestRepository;
import com.example.revolution.quest.state.QuestEntry;
import com.example.revolution.quest.state.QuestPlayerState;
import com.mongodb.client.model.UpdateOptions;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;

/** Backward-compatible permanent Quest state embedded in the existing players document. */
public final class MongoQuestRepository implements QuestRepository {
    private static final int SCHEMA_VERSION = 1;
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoQuestRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
    }

    @Override public QuestPlayerState load(UUID playerUuid) {
        if (!enabled || !connection.isConnected()) return new QuestPlayerState();
        Document player = connection.collection("players").find(new Document("_id", playerUuid.toString())).first();
        return player == null ? new QuestPlayerState() : decode(player.get("questSystem", Document.class));
    }

    @Override public void save(UUID playerUuid, QuestPlayerState state) {
        if (!enabled || !connection.isConnected()) return;
        connection.collection("players").updateOne(new Document("_id", playerUuid.toString()),
                new Document("$set", new Document("questSystem", encode(state))), new UpdateOptions().upsert(true));
    }

    static Document encode(QuestPlayerState state) {
        List<Document> board = new ArrayList<>();
        for (QuestEntry entry : state.boardEntries) {
            Document filters = new Document();
            for (Map.Entry<String, String> filter : entry.filters.entrySet()) {
                filters.append(filter.getKey(), filter.getValue());
            }
            board.add(new Document("entryId", entry.entryId)
                    .append("templateId", entry.templateId)
                    .append("eventType", entry.eventType == null ? null : entry.eventType.name())
                    .append("filters", filters)
                    .append("target", exact(entry.target))
                    .append("progress", exact(entry.progress))
                    .append("state", entry.state.name())
                    .append("crystalReward", entry.crystalReward)
                    .append("difficultyTier", entry.difficultyTier));
        }
        return new Document("schemaVersion", SCHEMA_VERSION)
                .append("boardEntries", board)
                .append("batchCooldownUntil", Math.max(0L, state.batchCooldownUntil))
                .append("completedQuestCount", Math.max(0L, state.completedQuestCount))
                .append("rerollDayId", state.rerollDayId)
                .append("rerollsUsed", Math.max(0, state.rerollsUsed));
    }

    @SuppressWarnings("unchecked")
    static QuestPlayerState decode(Document root) {
        QuestPlayerState state = new QuestPlayerState();
        if (root == null) return state;
        state.batchCooldownUntil = Math.max(0L, number(root.get("batchCooldownUntil")));
        state.completedQuestCount = Math.max(0L, number(root.get("completedQuestCount")));
        state.rerollDayId = root.getString("rerollDayId");
        state.rerollsUsed = Math.max(0, root.getInteger("rerollsUsed", 0));
        List<Document> board = (List<Document>) root.get("boardEntries");
        if (board == null) return state;
        for (Document stored : board) {
            try {
                QuestEntry entry = new QuestEntry();
                String entryId = stored.getString("entryId");
                if (entryId != null && !entryId.trim().isEmpty()) entry.entryId = entryId;
                entry.templateId = stored.getString("templateId");
                entry.eventType = GameplayEventType.valueOf(stored.getString("eventType"));
                Document filters = stored.get("filters", Document.class);
                if (filters != null) for (Map.Entry<String, Object> filter : filters.entrySet()) entry.filters.put(filter.getKey(), String.valueOf(filter.getValue()));
                entry.target = positive(stored.get("target"), BigDecimal.ONE);
                entry.progress = nonNegative(stored.get("progress"));
                entry.state = QuestState.valueOf(stored.getString("state"));
                entry.crystalReward = Math.max(0, stored.getInteger("crystalReward", 0));
                entry.difficultyTier = Math.max(0, stored.getInteger("difficultyTier", 0));
                state.boardEntries.add(entry);
            } catch (RuntimeException ignored) { }
        }
        return state;
    }

    private static long number(Object value) { return value instanceof Number ? ((Number) value).longValue() : 0L; }
    private static BigDecimal positive(Object value, BigDecimal fallback) {
        try { BigDecimal parsed = new BigDecimal(String.valueOf(value)); return parsed.signum() > 0 ? parsed : fallback; } catch (RuntimeException ignored) { return fallback; }
    }
    private static BigDecimal nonNegative(Object value) {
        try { return new BigDecimal(String.valueOf(value)).max(BigDecimal.ZERO); } catch (RuntimeException ignored) { return BigDecimal.ZERO; }
    }
    private static String exact(BigDecimal value) { return value == null || value.signum() == 0 ? "0" : value.stripTrailingZeros().toPlainString(); }
}
