package com.example.revolution.admin.service;

import com.example.revolution.admin.definition.AdminStageSnapshot;
import com.example.revolution.admin.definition.StagePatch;
import com.example.revolution.admin.repository.AdminStageStore;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.resource.definition.ResourceDefinitions;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** One shared validation/preview/apply path for single edits, multi-select and chat batch input. */
public final class AdminStageBatchService {
    private final AdminStageStore store;
    private final Supplier<List<AdminStageSnapshot>> snapshots;
    private final Runnable reload;
    private final Map<String, Prepared> prepared = new LinkedHashMap<>();
    public AdminStageBatchService(AdminStageStore store, Supplier<List<AdminStageSnapshot>> snapshots, Runnable reload) {
        this.store = store; this.snapshots = snapshots; this.reload = reload;
    }
    public StageList list() { store.recover(); return new StageList(store.revision(), snapshots.get()); }

    public Preview preview(String owner, String expectedRevision, List<StagePatch> requested) {
        if (requested == null || requested.isEmpty() || requested.size() > 100) throw new IllegalArgumentException("Batch must contain 1..100 stages");
        store.recover();
        String revision = store.revision();
        if (!revision.equals(expectedRevision)) throw new IllegalArgumentException("Configuration changed; reload before preparing changes");
        Map<String, AdminStageSnapshot> available = new LinkedHashMap<>();
        for (AdminStageSnapshot row : snapshots.get()) available.put(key(row.location, row.stage), row);
        Set<String> seen = new HashSet<>();
        List<AdminStageSnapshot> before = new ArrayList<>(), after = new ArrayList<>();
        List<StagePatch> patches = new ArrayList<>();
        for (StagePatch patch : requested) {
            String key = key(patch.location, patch.stage);
            if (!seen.add(key)) throw new IllegalArgumentException("Duplicate stage in batch");
            AdminStageSnapshot row = available.get(key);
            if (row == null || !row.problem.isEmpty()) throw new IllegalArgumentException("Missing or non-editable stage " + key);
            if (patch.hp == null && patch.population == null && patch.resource == null && patch.amount == null) throw new IllegalArgumentException("No selected fields");
            String hp = patch.hp == null ? row.hp : positive(patch.hp);
            String amount = patch.amount == null ? row.amount : positive(patch.amount);
            String resource = patch.resource == null ? row.resource : ResourceDefinitions.findCanonicalName(patch.resource);
            if (resource == null) throw new IllegalArgumentException("Unknown reward resource");
            int population = patch.population == null ? row.population : patch.population;
            if (patch.population != null && (population < 1 || population > 500)) throw new IllegalArgumentException("Population must be 1..500");
            if (patch.population != null && row.population == 0) throw new IllegalArgumentException("Stage has no spawn point to reuse");
            before.add(row);
            after.add(new AdminStageSnapshot(row.location, row.stage, row.internalId, population, row.mob,
                    hp, resource, amount, row.kind, row.problem, row.boss, row.persistedBoss, row.x, row.y, row.z, row.radius));
            patches.add(new StagePatch(row.location, row.stage, patch.hp == null ? null : hp,
                    patch.population, patch.resource == null ? null : resource, patch.amount == null ? null : amount));
        }
        long now = System.currentTimeMillis();
        prepared.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
        prepared.entrySet().removeIf(entry -> entry.getValue().owner.equals(owner));
        if (prepared.size() >= 16) throw new IllegalArgumentException("Too many pending previews");
        String token = UUID.randomUUID().toString();
        long expiresAt = now + 120_000L;
        prepared.put(token, new Prepared(owner, revision, patches, expiresAt));
        return new Preview(token, revision, expiresAt, before, after);
    }

    public void apply(String owner, String token) {
        Prepared change = prepared.get(token);
        if (change == null || !change.owner.equals(owner)) throw new IllegalArgumentException("Unknown preview or different session");
        prepared.remove(token); // Never automatically retry a mutation with an uncertain result.
        if (change.expiresAt < System.currentTimeMillis()) throw new IllegalArgumentException("Preview expired");
        try { store.commit(change.revision, change.patches); }
        catch (RuntimeException failure) {
            // Complete/recover the durable decision before any runtime reload after an uncertain marker/write.
            try { store.recover(); reload.run(); }
            catch (RuntimeException recovery) { failure.addSuppressed(recovery); }
            prepared.clear();
            throw failure;
        }
        prepared.clear();
        reload.run(); // Once per successful batch, after the durable commit marker.
    }

    private static String positive(String raw) {
        if (raw.length() > 128) throw new IllegalArgumentException("Number too long");
        BigDecimal value = LargeNumbers.parseExact(raw);
        if (value.signum() <= 0 || value.precision() > 100 || Math.abs((long)value.scale()) > 10000) throw new IllegalArgumentException("Number out of bounds");
        return value.toString(); // Exact scientific representation keeps very large values bounded.
    }
    private static String key(int location, int stage) { return location + ":" + stage; }
    private static final class Prepared {
        final String owner, revision; final List<StagePatch> patches; final long expiresAt;
        Prepared(String owner, String revision, List<StagePatch> patches, long expiresAt) {
            this.owner = owner; this.revision = revision; this.patches = patches; this.expiresAt = expiresAt;
        }
    }
    public static final class StageList {
        public final String revision; public final List<AdminStageSnapshot> stages;
        StageList(String revision, List<AdminStageSnapshot> stages) { this.revision = revision; this.stages = stages; }
    }
    public static final class Preview {
        public final String token, revision;
        public final long expiresAt;
        public final List<AdminStageSnapshot> before, after;
        public final String populationNote = "Existing spawn points are retained; additional slots reuse an existing fallback point in RANDOM_AREA mode.";
        Preview(String token, String revision, long expiresAt, List<AdminStageSnapshot> before, List<AdminStageSnapshot> after) {
            this.token = token; this.revision = revision; this.expiresAt = expiresAt; this.before = before; this.after = after;
        }
    }
}
