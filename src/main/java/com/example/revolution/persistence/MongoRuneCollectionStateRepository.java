package com.example.revolution.persistence;

import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import com.mongodb.client.model.UpdateOptions;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bson.Document;

/** Additive v2.3.0 metadata; legacy decks/cards remain untouched for backward compatibility. */
public final class MongoRuneCollectionStateRepository implements RuneCollectionStateRepository {
    private static final String ROOT = "runeCollections";
    private static final String UNLOCKED = "unlockedCollections";

    private final MongoConnection connection;
    private final boolean enabled;

    public MongoRuneCollectionStateRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<RuneCollectionId> getUnlockedCollections(UUID uuid) {
        EnumSet<RuneCollectionId> result = EnumSet.of(RuneCollectionId.COLLECTION_01);
        if (!enabled || !connection.isConnected() || uuid == null) return Collections.unmodifiableSet(result);

        Document player = connection.collection("players")
                .find(new Document("_id", uuid.toString())).first();
        if (player == null) return Collections.unmodifiableSet(result);

        Document root = player.get(ROOT, Document.class);
        if (root == null) return Collections.unmodifiableSet(result);
        Object raw = root.get(UNLOCKED);
        if (!(raw instanceof List)) return Collections.unmodifiableSet(result);

        for (Object value : (List<Object>) raw) {
            RuneCollectionId id = RuneCollectionId.fromId(String.valueOf(value));
            if (id != null) result.add(id);
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public boolean unlockCollection(UUID uuid, RuneCollectionId collectionId) {
        if (collectionId == null || uuid == null) return false;
        if (collectionId == RuneCollectionId.COLLECTION_01) return false;
        if (!enabled || !connection.isConnected()) return false;
        if (getUnlockedCollections(uuid).contains(collectionId)) return false;

        connection.collection("players").updateOne(
                new Document("_id", uuid.toString()),
                new Document("$addToSet", new Document(ROOT + "." + UNLOCKED, collectionId.id()))
                        .append("$setOnInsert", new Document("activeDeck", 0)),
                new UpdateOptions().upsert(true));
        return true;
    }
}
