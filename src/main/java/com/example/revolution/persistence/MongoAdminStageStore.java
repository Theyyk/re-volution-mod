package com.example.revolution.persistence;

import com.example.revolution.admin.definition.StagePatch;
import com.example.revolution.admin.repository.AdminStageStore;
import com.example.revolution.admin.service.RecoverableAdminCommit;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.service.StageMobBootstrapService;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.bson.Document;

/** Bounded, journaled multi-document writes for the existing standalone Mongo deployment. */
public final class MongoAdminStageStore implements AdminStageStore {
    private final MongoConnection connection;
    private final boolean enabled;
    public MongoAdminStageStore(MongoConnection connection, boolean enabled) { this.connection = connection; this.enabled = enabled; }
    private MongoCollection<Document> collection(String name) {
        if (!enabled || !connection.isConnected()) throw new IllegalStateException("Persistent gameplay storage unavailable");
        return connection.collection(name).withWriteConcern(WriteConcern.JOURNALED);
    }
    private MongoCollection<Document> journal() { return collection("admin_stage_commit"); }

    @Override public String revision() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String name : new String[] { "custom_locations", "custom_mobs", "custom_mob_spawns" }) {
                digest.update(name.getBytes(StandardCharsets.UTF_8));
                int count = 0;
                for (Document document : collection(name).find().sort(Sorts.ascending("_id"))) {
                    if (++count > 20000) throw new IllegalArgumentException("Configuration is too large for desktop editing");
                    digest.update(canonical(document).getBytes(StandardCharsets.UTF_8)); digest.update((byte)0);
                }
            }
            StringBuilder value = new StringBuilder();
            for (byte part : digest.digest()) value.append(String.format("%02x", part & 255));
            return value.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    @Override public void recover() {
        Document pending = journal().find(new Document("_id", "active")).first();
        if (pending == null || !"PENDING".equals(pending.getString("state"))) return;
        List<Document> changes = pending.getList("changes", Document.class);
        if (changes == null || changes.isEmpty()) throw new IllegalStateException("Invalid pending stage journal; recovery required");
        restore(changes, "before");
        mark("REVERTED");
    }

    @Override public void commit(String expectedRevision, List<StagePatch> patches) {
        recover();
        if (!revision().equals(expectedRevision)) throw new IllegalArgumentException("Configuration changed after preview");
        List<Document> changes = new ArrayList<>();
        for (StagePatch patch : patches) {
            int internalId = MobZoneIds.encode(patch.location, patch.stage);
            String mob = StageMobBootstrapService.canonicalMobName(patch.location, patch.stage);
            List<Document> found = collection("custom_mobs").find(new Document("name", mob)).limit(2).into(new ArrayList<>());
            if (found.size() != 1) throw new IllegalArgumentException("Missing or duplicate canonical mob");
            List<Document> beforeSpawns = collection("custom_mob_spawns").find(new Document("locationId", internalId))
                    .sort(Sorts.ascending("spawnId")).into(new ArrayList<>());
            Document beforeMob = found.get(0);
            Document afterMob = copy(beforeMob);
            if (patch.hp != null) afterMob.put("hp", patch.hp);
            if (patch.resource != null) afterMob.put("resource", patch.resource);
            if (patch.amount != null) afterMob.put("amount", patch.amount);
            List<Document> afterSpawns = patch.population == null ? beforeSpawns : population(beforeSpawns, patch.population);
            changes.add(new Document("mob", mob).append("locationId", internalId)
                    .append("beforeMob", beforeMob).append("afterMob", afterMob)
                    .append("beforeSpawns", beforeSpawns).append("afterSpawns", afterSpawns)
                    .append("populationChanged", patch.population != null));
        }
        Document record = new Document("_id", "active").append("state", "PENDING")
                .append("revision", expectedRevision).append("changes", changes);
        if (record.toJson().getBytes(StandardCharsets.UTF_8).length > 4 * 1024 * 1024) throw new IllegalArgumentException("Batch journal too large; select fewer stages");
        RecoverableAdminCommit.execute(new RecoverableAdminCommit.Port() {
            public void pending() { journal().replaceOne(new Document("_id", "active"), record, new ReplaceOptions().upsert(true)); }
            public void after() { restore(changes, "after"); }
            public void before() { restore(changes, "before"); }
            public void committed() { mark("COMMITTED"); }
            public void reverted() { mark("REVERTED"); }
        });
    }

    private void mark(String state) {
        if (journal().updateOne(new Document("_id", "active"), new Document("$set", new Document("state", state))).getMatchedCount() != 1)
            throw new IllegalStateException("Stage journal marker unavailable");
    }
    private void restore(List<Document> changes, String side) {
        for (Document change : changes) {
            Document mob = change.get(side + "Mob", Document.class);
            collection("custom_mobs").replaceOne(new Document("_id", mob.get("_id")), mob, new ReplaceOptions().upsert(true));
            if (!change.getBoolean("populationChanged", false)) continue;
            int locationId = change.getInteger("locationId");
            collection("custom_mob_spawns").deleteMany(new Document("locationId", locationId));
            List<Document> spawns = change.getList(side + "Spawns", Document.class);
            if (!spawns.isEmpty()) {
                List<Document> fresh = new ArrayList<>(); for (Document spawn : spawns) fresh.add(copy(spawn));
                collection("custom_mob_spawns").insertMany(fresh);
            }
        }
    }

    static List<Document> population(List<Document> previous, int count) {
        if (count < 1 || count > 500) throw new IllegalArgumentException("Population outside 1..500");
        List<Document> enabled = new ArrayList<>(), result = new ArrayList<>();
        int maximumId = 0;
        for (Document spawn : previous) {
            maximumId = Math.max(maximumId, spawn.getInteger("spawnId"));
            if (spawn.getBoolean("enabled", false)) enabled.add(spawn); else result.add(copy(spawn));
        }
        if (enabled.isEmpty()) throw new IllegalArgumentException("An existing spawn fallback is required");
        for (int i = 0; i < count; i++) {
            Document spawn = copy(enabled.get(Math.min(i, enabled.size() - 1)));
            if (i >= enabled.size()) {
                if (maximumId == Integer.MAX_VALUE) throw new IllegalArgumentException("Spawn identity limit reached");
                spawn.remove("_id"); spawn.put("spawnId", ++maximumId); spawn.put("spawnMode", "RANDOM_AREA");
            }
            result.add(spawn);
        }
        return result;
    }
    private static Document copy(Document document) { return Document.parse(document.toJson()); }
    private static String canonical(Document document) {
        Document ordered = new Document();
        for (String key : new TreeMap<>(document).keySet()) ordered.put(key, document.get(key));
        return ordered.toJson();
    }
}
