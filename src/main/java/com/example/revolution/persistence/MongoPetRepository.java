package com.example.revolution.persistence;

import com.example.revolution.pet.definition.PetRank;
import com.example.revolution.pet.repository.PetRepository;
import com.example.revolution.pet.state.PetPlayerState;
import com.mongodb.client.model.UpdateOptions;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;

/** Backward-compatible Pet persistence embedded in the existing players document. */
public final class MongoPetRepository implements PetRepository {
    private static final int SCHEMA_VERSION = 2;
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoPetRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
    }

    @Override public PetPlayerState load(UUID playerUuid) {
        if (!enabled || !connection.isConnected()) return new PetPlayerState();
        Document player = connection.collection("players").find(new Document("_id", playerUuid.toString())).first();
        return player == null ? new PetPlayerState() : decode(player.get("petSystem", Document.class));
    }

    @Override public void save(UUID playerUuid, PetPlayerState state) {
        if (!enabled || !connection.isConnected()) return;
        connection.collection("players").updateOne(new Document("_id", playerUuid.toString()),
                new Document("$set", new Document("petSystem", encode(state))), new UpdateOptions().upsert(true));
    }

    /** Prestige transformation used by MongoPrestigeRepository inside the same guarded reset write. */
    public static Document resetForPrestige(Document player) {
        if (player == null) return null;
        PetPlayerState state = decode(player.get("petSystem", Document.class));
        PetPlayerState next = new PetPlayerState();
        next.premiumPetSlotsUnlocked = state.premiumPetSlotsUnlocked;
        for (Map.Entry<String, PetPlayerState.PetProgress> entry : state.pets.entrySet()) {
            if (entry.getValue().rank.isPersistentAcrossPrestige()) next.pets.put(entry.getKey(), entry.getValue().copy());
        }
        PetPlayerState.RunState preserved = new PetPlayerState.RunState();
        if (state.run != null) {
            for (PetPlayerState.SelectedPet selected : state.run.selectedPets) {
                PetPlayerState.PetProgress progress = next.pets.get(selected.petId);
                if (progress != null && progress.rank.isPersistentAcrossPrestige()) preserved.selectedPets.add(selected.copy());
            }
        }
        sortAndCompact(preserved, next);
        next.run = preserved;
        return encode(next);
    }

    private static void sortAndCompact(PetPlayerState.RunState run, PetPlayerState state) {
        run.selectedPets.sort((a, b) -> {
            PetRank ar = state.pets.containsKey(a.petId) ? state.pets.get(a.petId).rank : PetRank.ORDINARY;
            PetRank br = state.pets.containsKey(b.petId) ? state.pets.get(b.petId).rank : PetRank.ORDINARY;
            int rank = Integer.compare(br.progressionOrder(), ar.progressionOrder());
            if (rank != 0) return rank;
            int order = Long.compare(a.order, b.order);
            return order != 0 ? order : a.petId.compareTo(b.petId);
        });
    }

    public static Document encode(PetPlayerState state) {
        Document root = new Document("schemaVersion", SCHEMA_VERSION)
                .append("premiumPetSlotsUnlocked", Math.max(0, Math.min(2, state.premiumPetSlotsUnlocked)));
        Document pets = new Document();
        for (Map.Entry<String, PetPlayerState.PetProgress> entry : state.pets.entrySet()) {
            PetPlayerState.PetProgress p = entry.getValue();
            pets.append(entry.getKey(), new Document("rank", p.rank.name())
                    .append("level", p.level.max(BigInteger.ONE).toString())
                    .append("xp", exact(p.xp))
                    .append("appliedLevelXp", exact(p.appliedLevelXp)));
        }
        root.append("pets", pets);
        if (state.run != null) {
            Document run = new Document();
            if (state.run.runId != null) run.append("runId", state.run.runId);
            run.append("purchasedPetIds", new ArrayList<>(state.run.purchasedPetIds));
            List<Document> selected = new ArrayList<>();
            for (PetPlayerState.SelectedPet pet : state.run.selectedPets) selected.add(new Document("petId", pet.petId).append("premiumSlot", pet.premiumSlot).append("order", pet.order));
            run.append("selectedPets", selected)
                    .append("normalSlotsUnlockedThisRun", state.run.normalSlotsUnlockedThisRun)
                    .append("totalNormalSlotPurchasesThisRun", state.run.totalNormalSlotPurchasesThisRun)
                    .append("lastPetXpAccrualAt", state.run.lastPetXpAccrualAt);
            Document cooldowns = new Document();
            for (Map.Entry<String, Long> entry : state.run.cooldownUntil.entrySet()) cooldowns.append(entry.getKey(), entry.getValue());
            run.append("cooldownUntil", cooldowns);
            root.append("run", run);
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    public static PetPlayerState decode(Document root) {
        PetPlayerState state = new PetPlayerState();
        if (root == null) return state;
        state.premiumPetSlotsUnlocked = Math.max(0, Math.min(2, root.getInteger("premiumPetSlotsUnlocked", 0)));
        Document pets = root.get("pets", Document.class);
        if (pets != null) for (String petId : pets.keySet()) {
            Document stored = pets.get(petId, Document.class);
            if (stored == null) continue;
            PetPlayerState.PetProgress p = new PetPlayerState.PetProgress();
            p.rank = PetRank.parse(stored.getString("rank"));
            try { p.level = new BigInteger(String.valueOf(stored.get("level"))).max(BigInteger.ONE); } catch (RuntimeException ignored) { p.level = BigInteger.ONE; }
            try { p.xp = new BigDecimal(String.valueOf(stored.get("xp"))).max(BigDecimal.ZERO); } catch (RuntimeException ignored) { p.xp = BigDecimal.ZERO; }
            try { p.appliedLevelXp = new BigDecimal(String.valueOf(stored.get("appliedLevelXp"))).max(BigDecimal.ZERO); } catch (RuntimeException ignored) { p.appliedLevelXp = BigDecimal.ZERO; }
            state.pets.put(petId, p);
        }
        Document runDoc = root.get("run", Document.class);
        if (runDoc != null) {
            PetPlayerState.RunState run = new PetPlayerState.RunState();
            run.runId = runDoc.getString("runId");
            List<String> purchased = (List<String>) runDoc.get("purchasedPetIds");
            if (purchased != null) run.purchasedPetIds.addAll(purchased);
            List<Document> selected = (List<Document>) runDoc.get("selectedPets");
            if (selected != null) for (Document s : selected) {
                String id = s.getString("petId");
                if (id != null) run.selectedPets.add(new PetPlayerState.SelectedPet(id, Boolean.TRUE.equals(s.getBoolean("premiumSlot")), number(s.get("order"))));
            }
            run.normalSlotsUnlockedThisRun = Math.max(0, Math.min(8, runDoc.getInteger("normalSlotsUnlockedThisRun", 0)));
            run.totalNormalSlotPurchasesThisRun = Math.max(0L, number(runDoc.get("totalNormalSlotPurchasesThisRun")));
            run.lastPetXpAccrualAt = Math.max(0L, number(runDoc.get("lastPetXpAccrualAt")));
            Document cooldowns = runDoc.get("cooldownUntil", Document.class);
            if (cooldowns != null) for (String id : cooldowns.keySet()) run.cooldownUntil.put(id, Math.max(0L, number(cooldowns.get(id))));
            state.run = run;
        }
        return state;
    }

    private static long number(Object value) { return value instanceof Number ? ((Number) value).longValue() : 0L; }
    private static String exact(BigDecimal value) { return value == null || value.signum() == 0 ? "0" : value.stripTrailingZeros().toPlainString(); }
}
