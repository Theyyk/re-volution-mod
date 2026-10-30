package com.example.revolution.persistence;

import com.example.revolution.RevolutionMod;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.repository.PlayerIdentity;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneStorageRules;
import com.mongodb.client.model.UpdateOptions;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bson.Document;

public final class MongoPlayerRepository implements PlayerRepository, RuneCollectionStateRepository {
    private static final String RUNE_ROOT = "runeCollections";
    private static final String FIXED_COLLECTIONS = "collections";
    private static final String FIXED_SLOTS = "slots";

    private final MongoConnection connection;
    private final boolean enabled;
    private final RuneCollectionStateRepository runeCollectionStates;

    public MongoPlayerRepository(MongoConnection connection) { this(connection, true); }

    /** The old core repositories were available only after physical-server preInit. */
    public MongoPlayerRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
        this.runeCollectionStates = new MongoRuneCollectionStateRepository(connection, enabled);
    }

    @Override
    public int getBalance(UUID uuid) {
        if (!enabled || !connection.isConnected()) return 0;
        Document doc = connection.collection("players").find(new Document("_id", uuid.toString())).first();
        return doc == null ? 0 : legacyInt(doc.get("balance"));
    }

    @Override
    public void setBalance(UUID uuid, int balance) {
        if (!enabled || !connection.isConnected()) return;
        connection.collection("players").updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("balance", balance)), new UpdateOptions().upsert(true));
    }

    @Override
    public int getCrystals(UUID uuid) {
        if (!enabled || !connection.isConnected()) return 0;
        Document doc = connection.collection("players").find(new Document("_id", uuid.toString())).first();
        return doc == null ? 0 : legacyInt(doc.get("crystals"));
    }

    @Override
    public void setCrystals(UUID uuid, int crystals) {
        if (!enabled || !connection.isConnected()) return;
        connection.collection("players").updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("crystals", crystals)), new UpdateOptions().upsert(true));
    }

    private static int legacyInt(Object raw) {
        if (raw == null) return 0;
        try {
            BigDecimal value = raw instanceof Number ? new BigDecimal(raw.toString()) : new BigDecimal(String.valueOf(raw));
            if (value.signum() <= 0) return 0;
            if (value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) >= 0) return Integer.MAX_VALUE;
            return value.intValue();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RuneRecord> getRunesInCollection(UUID uuid, int collectionIndex) {
        List<RuneRecord> result = new ArrayList<>();
        if (!enabled || !connection.isConnected()) return result;
        Document doc = connection.collection("players").find(new Document("_id", uuid.toString())).first();
        if (doc == null) return result;

        if (isAdditiveFixedCollection(collectionIndex)) {
            List<Document> runes = fixedRunes(doc, collectionIndex);
            if (runes == null) return result;
            appendRuneRecords(result, runes);
            return result;
        }
        if (collectionIndex != 0) return result;

        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null || collections.isEmpty()) {
            RevolutionMod.logger.warn("getRunesInCollection: legacy collection 0 not found");
            return result;
        }
        Document collection = collections.get(0);
        List<Document> runes = (List<Document>) collection.get("cards");
        if (runes == null) {
            RevolutionMod.logger.info("getRunesInCollection: legacy collection 0 has no rune storage");
            return result;
        }
        appendRuneRecords(result, runes);
        return result;
    }

    @Override
    public int getActiveCollection(UUID uuid) {
        if (!enabled || !connection.isConnected()) return 0;
        Document doc = connection.collection("players").find(new Document("_id", uuid.toString())).first();
        if (doc == null) return 0;
        Integer d = doc.getInteger("activeDeck", 0);
        return d == null ? 0 : d;
    }

    @Override
    public void setActiveCollection(UUID uuid, int collectionIndex) {
        if (!enabled || !connection.isConnected()) return;
        connection.collection("players").updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("activeDeck", collectionIndex)), new UpdateOptions().upsert(true));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getCollectionNames(UUID uuid) {
        List<String> result = new ArrayList<>();
        if (!enabled || !connection.isConnected()) return result;
        Document doc = connection.collection("players").find(new Document("_id", uuid.toString())).first();
        if (doc == null) return result;
        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null) return result;
        for (Document d : collections) result.add(d.getString("name"));
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addCollection(UUID uuid, String name) {
        if (!enabled || !connection.isConnected()) return;
        Document doc = connection.collection("players").find(new Document("_id", uuid.toString())).first();
        if (doc == null) {
            List<Document> collections = new ArrayList<>();
            collections.add(new Document("name", name).append("cards", new ArrayList<Document>()));
            connection.collection("players").insertOne(new Document("_id", uuid.toString()).append("balance", 0).append("crystals", 0)
                    .append("activeDeck", 0).append("decks", collections));
            RevolutionMod.logger.info("addCollection: created player doc + collection '" + name + "'");
            return;
        }
        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null) collections = new ArrayList<>();
        collections.add(new Document("name", name).append("cards", new ArrayList<Document>()));
        connection.collection("players").updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", collections)), new UpdateOptions().upsert(true));
        RevolutionMod.logger.info("addCollection: added collection '" + name + "', total collections: " + collections.size());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void deleteCollection(UUID uuid, int collectionIndex) {
        if (!enabled || !connection.isConnected()) return;
        Document doc = connection.collection("players").find(new Document("_id", uuid.toString())).first();
        if (doc == null) return;
        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null || collectionIndex >= collections.size()) return;
        collections.remove(collectionIndex);
        int active = getActiveCollection(uuid);
        if (active >= collections.size()) active = 0;
        connection.collection("players").updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", collections).append("activeDeck", active)), new UpdateOptions().upsert(true));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addRuneToCollection(UUID uuid, int collectionIndex, int slot, int runeIndex, int layer, int level) {
        if (!enabled || !connection.isConnected()) {
            RevolutionMod.logger.warn("addRuneToCollection: !connection.isConnected()");
            return;
        }
        Document rune = runeDocument(slot, runeIndex, layer, level);
        if (isAdditiveFixedCollection(collectionIndex)) {
            String path = fixedSlotsPath(collectionIndex);
            connection.collection("players").updateOne(
                    new Document("_id", uuid.toString()),
                    new Document("$push", new Document(path, rune))
                            .append("$setOnInsert", new Document("activeDeck", 0)),
                    new UpdateOptions().upsert(true));
            RevolutionMod.logger.debug("addRuneToCollection: added rune to fixed collection " + collectionIndex + ", slot=" + slot);
            return;
        }
        if (collectionIndex != 0) return;

        Document doc = connection.collection("players").find(new Document("_id", uuid.toString())).first();
        if (doc == null) {
            RevolutionMod.logger.warn("addRuneToCollection: player document not found for " + uuid);
            return;
        }
        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null || collections.isEmpty()) {
            RevolutionMod.logger.warn("addRuneToCollection: legacy collection 0 not found");
            return;
        }
        Document collection = collections.get(0);
        List<Document> runes = (List<Document>) collection.get("cards");
        if (runes == null) runes = new ArrayList<>();
        runes.add(rune);
        collection.put("cards", runes);
        collections.set(0, collection);
        connection.collection("players").updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", collections)), new UpdateOptions().upsert(true));
        RevolutionMod.logger.debug("addRuneToCollection: added rune to legacy collection 0 (total=" + runes.size() + ", slot=" + slot + ")");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean setRuneRank(UUID uuid, int collectionIndex, int runeIndex, int layer) {
        if (!enabled || !connection.isConnected()) return false;
        Document doc = connection.collection("players").find(new Document("_id", uuid.toString())).first();
        if (doc == null) return false;

        if (isAdditiveFixedCollection(collectionIndex)) {
            List<Document> runes = fixedRunes(doc, collectionIndex);
            if (runes == null) return false;
            Document target = highestRankRecord(runes, runeIndex);
            if (target == null) return false;
            normalizeRankRecord(target, runeIndex, layer);
            connection.collection("players").updateOne(
                    new Document("_id", uuid.toString()),
                    new Document("$set", new Document(fixedSlotsPath(collectionIndex), runes)),
                    new UpdateOptions().upsert(true));
            RevolutionMod.logger.info("setRuneRank: fixed collection=" + collectionIndex + ", rune=" + runeIndex + ", rank=" + layer);
            return true;
        }
        if (collectionIndex != 0) return false;

        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null || collections.isEmpty()) return false;
        Document collection = collections.get(0);
        List<Document> runes = (List<Document>) collection.get("cards");
        if (runes == null) return false;
        Document target = highestRankRecord(runes, runeIndex);
        if (target == null) return false;
        normalizeRankRecord(target, runeIndex, layer);
        collection.put("cards", runes);
        collections.set(0, collection);
        connection.collection("players").updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", collections)), new UpdateOptions().upsert(true));
        RevolutionMod.logger.info("setRuneRank: legacy collection=0, rune=" + runeIndex + ", rank=" + layer);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void clearCollection(UUID uuid, int collectionIndex) {
        if (!enabled || !connection.isConnected()) return;
        if (isAdditiveFixedCollection(collectionIndex)) {
            connection.collection("players").updateOne(
                    new Document("_id", uuid.toString()),
                    new Document("$set", new Document(fixedSlotsPath(collectionIndex), new ArrayList<Document>())),
                    new UpdateOptions().upsert(true));
            return;
        }
        if (collectionIndex != 0) return;

        Document doc = connection.collection("players").find(new Document("_id", uuid.toString())).first();
        if (doc == null) return;
        List<Document> collections = (List<Document>) doc.get("decks");
        if (collections == null || collections.isEmpty()) return;
        collections.get(0).put("cards", new ArrayList<Document>());
        connection.collection("players").updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("decks", collections)), new UpdateOptions().upsert(true));
    }

    @Override
    public Set<RuneCollectionId> getUnlockedCollections(UUID uuid) {
        return runeCollectionStates.getUnlockedCollections(uuid);
    }

    @Override
    public boolean unlockCollection(UUID uuid, RuneCollectionId collectionId) {
        return runeCollectionStates.unlockCollection(uuid, collectionId);
    }

    @Override
    public List<PlayerIdentity> listKnownPlayers() {
        List<PlayerIdentity> result = new ArrayList<>();
        if (!enabled || !connection.isConnected()) return result;
        for (Document doc : connection.collection("players").find()) {
            String rawUuid = doc.getString("_id");
            if (rawUuid == null) continue;
            try {
                result.add(new PlayerIdentity(UUID.fromString(rawUuid), doc.getString("lastKnownName")));
            } catch (IllegalArgumentException ignored) {
                RevolutionMod.logger.warn("Ignoring players document with invalid UUID: " + rawUuid);
            }
        }
        return result;
    }

    @Override
    public void rememberIdentity(UUID uuid, String lastKnownName) {
        if (!enabled || !connection.isConnected() || uuid == null || lastKnownName == null || lastKnownName.trim().isEmpty()) return;
        connection.collection("players").updateOne(new Document("_id", uuid.toString()),
                new Document("$set", new Document("lastKnownName", lastKnownName.trim())), new UpdateOptions().upsert(true));
    }

    @SuppressWarnings("unchecked")
    private static List<Document> fixedRunes(Document player, int collectionIndex) {
        RuneCollectionId id = RuneCollectionId.fromLegacyIndex(collectionIndex);
        if (id == null || id == RuneCollectionId.COLLECTION_01) return null;
        Document root = player.get(RUNE_ROOT, Document.class);
        if (root == null) return null;
        Document collections = root.get(FIXED_COLLECTIONS, Document.class);
        if (collections == null) return null;
        Document collection = collections.get(id.id(), Document.class);
        if (collection == null) return null;
        Object raw = collection.get(FIXED_SLOTS);
        return raw instanceof List ? (List<Document>) raw : null;
    }

    private static boolean isAdditiveFixedCollection(int collectionIndex) {
        RuneCollectionId id = RuneCollectionId.fromLegacyIndex(collectionIndex);
        return id != null && id != RuneCollectionId.COLLECTION_01;
    }

    private static String fixedSlotsPath(int collectionIndex) {
        RuneCollectionId id = RuneCollectionId.fromLegacyIndex(collectionIndex);
        if (id == null || id == RuneCollectionId.COLLECTION_01) {
            throw new IllegalArgumentException("fixed rune collection index required");
        }
        return RUNE_ROOT + "." + FIXED_COLLECTIONS + "." + id.id() + "." + FIXED_SLOTS;
    }

    private static Document runeDocument(int slot, int runeIndex, int layer, int level) {
        return new Document("slot", slot)
                .append("cardIndex", runeIndex)
                .append("layer", RuneStorageRules.clampGameplayRank(layer))
                .append("level", Math.max(1, level));
    }

    private static void appendRuneRecords(List<RuneRecord> out, List<Document> runes) {
        for (Document rune : runes) {
            out.add(new RuneRecord(
                    rune.getInteger("slot", 0),
                    rune.getInteger("cardIndex", 0),
                    rune.getInteger("layer", 1),
                    rune.getInteger("level", 1)));
        }
    }

    private static Document highestRankRecord(List<Document> runes, int runeIndex) {
        Document target = null;
        for (Document rune : runes) {
            if (rune.getInteger("cardIndex", -1) != runeIndex) continue;
            if (target == null || rune.getInteger("layer", 1) > target.getInteger("layer", 1)) target = rune;
        }
        return target;
    }

    private static void normalizeRankRecord(Document target, int runeIndex, int layer) {
        target.put("slot", runeIndex);
        target.put("cardIndex", runeIndex);
        target.put("layer", RuneStorageRules.clampGameplayRank(layer));
        if (!target.containsKey("level")) target.put("level", 1);
    }
}
