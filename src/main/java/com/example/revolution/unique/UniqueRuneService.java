package com.example.revolution.unique;

import com.example.revolution.gameplay.GameplayEvent;
import com.example.revolution.gameplay.GameplayEventSink;
import com.example.revolution.gameplay.GameplayEventType;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.unique.UniqueRuneModel.CollectionDefinition;
import com.example.revolution.unique.UniqueRuneModel.CollectionId;
import com.example.revolution.unique.UniqueRuneModel.CollectionState;
import com.example.revolution.unique.UniqueRuneModel.PlayerState;
import com.example.revolution.unique.UniqueRuneModel.Rarity;
import com.example.revolution.unique.UniqueRuneModel.RuneDefinition;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative acquisition, awakening, rollback and enhancement application service. */
public final class UniqueRuneService {
    public interface MilestoneProvider { long milestone(UUID playerUuid); }
    public interface GodAccessProvider { boolean unlocked(UUID playerUuid); }
    public interface OfferingsWallet {
        BigDecimal balance(UUID playerUuid);
        boolean spend(UUID playerUuid, BigDecimal amount);
    }
    public interface RandomSource { int nextInt(int bound); }

    public enum ResultCode {
        OK, DISABLED, LOCKED, INVALID, INSUFFICIENT_RESOURCE, DUPLICATE, COMPLETE, NOT_READY,
        MAX_RARITY, CONFIRMATION_REQUIRED, NO_ROLLBACK, DAILY_ALREADY_CLAIMED, REPLAYED
    }

    public static final class Result {
        public final ResultCode code;
        public final String runeId;
        public final boolean duplicate;
        public final BigDecimal compensation;
        private Result(ResultCode code, String runeId, boolean duplicate, BigDecimal compensation) {
            this.code = code; this.runeId = runeId == null ? "" : runeId; this.duplicate = duplicate;
            this.compensation = compensation == null ? BigDecimal.ZERO : compensation;
        }
        public boolean ok() { return code == ResultCode.OK || code == ResultCode.DUPLICATE; }
        static Result of(ResultCode code) { return new Result(code, "", false, BigDecimal.ZERO); }
        static Result grant(String runeId) { return new Result(ResultCode.OK, runeId, false, BigDecimal.ZERO); }
        static Result duplicate(String runeId, BigDecimal compensation) { return new Result(ResultCode.DUPLICATE, runeId, true, compensation); }
    }

    public static final class SharePayload {
        public final String runeId;
        public final CollectionId collectionId;
        public final Rarity rarity;
        public final int enhancement;
        public final Map<String, String> effects;
        SharePayload(String runeId, CollectionId collectionId, Rarity rarity, int enhancement, Map<String, String> effects) {
            this.runeId = runeId; this.collectionId = collectionId; this.rarity = rarity; this.enhancement = enhancement;
            this.effects = Collections.unmodifiableMap(new HashMap<>(effects));
        }
    }

    private final UniqueRuneRepository repository;
    private final BalanceUniqueRunePolicy policy;
    private final ResourceService resources;
    private final MilestoneProvider milestones;
    private final GodAccessProvider godAccess;
    private final OfferingsWallet offerings;
    private final RandomSource random;
    private final Map<UUID, Long> lastRequestNonce = new ConcurrentHashMap<>();
    private GameplayEventSink gameplayEvents = GameplayEventSink.NONE;

    public UniqueRuneService(UniqueRuneRepository repository, BalanceUniqueRunePolicy policy, ResourceService resources,
                             MilestoneProvider milestones, GodAccessProvider godAccess, OfferingsWallet offerings) {
        this(repository, policy, resources, milestones, godAccess, offerings, bound -> new Random().nextInt(bound));
    }

    public UniqueRuneService(UniqueRuneRepository repository, BalanceUniqueRunePolicy policy, ResourceService resources,
                             MilestoneProvider milestones, GodAccessProvider godAccess, OfferingsWallet offerings, RandomSource random) {
        if (repository == null || policy == null || resources == null || milestones == null || godAccess == null || offerings == null || random == null)
            throw new IllegalArgumentException("Unique Rune dependencies are required");
        this.repository = repository; this.policy = policy; this.resources = resources; this.milestones = milestones;
        this.godAccess = godAccess; this.offerings = offerings; this.random = random;
    }

    public void setGameplayEventSink(GameplayEventSink sink) { gameplayEvents = sink == null ? GameplayEventSink.NONE : sink; }
    public PlayerState snapshot(UUID playerUuid) { return repository.load(playerUuid).copy(); }
    public boolean isUnlocked(UUID playerUuid, CollectionId id) {
        if (playerUuid == null || id == null || !policy.collectionEnabled(id)) return false;
        if (id == CollectionId.OFFERINGS_COLLECTION) return godAccess.unlocked(playerUuid);
        return milestones.milestone(playerUuid) >= policy.unlockMilestone(id);
    }

    public synchronized Result purchaseCrystal(UUID playerUuid, long requestNonce) {
        if (!acceptNonce(playerUuid, requestNonce)) return Result.of(ResultCode.REPLAYED);
        if (!isUnlocked(playerUuid, CollectionId.CRYSTAL_COLLECTION)) return Result.of(ResultCode.LOCKED);
        PlayerState state = repository.load(playerUuid); CollectionState collection = state.collection(CollectionId.CRYSTAL_COLLECTION);
        if (collection.progress() >= UniqueRuneModel.RUNES_PER_COLLECTION) return Result.of(ResultCode.COMPLETE);
        BigDecimal cost = policy.crystalPurchaseCost(); if (!resources.spendPlayerResource(playerUuid, "crystals", cost)) return Result.of(ResultCode.INSUFFICIENT_RESOURCE);
        RuneDefinition rune = roll(policy.definition(CollectionId.CRYSTAL_COLLECTION).runes);
        boolean duplicate = collection.ownsAtCurrent(rune.runeId);
        if (!duplicate) collection.owned.put(rune.runeId, collection.currentRarity);
        repository.save(playerUuid, state);
        if (duplicate) {
            BigDecimal pearl = policy.duplicatePearl(); resources.grantPlayerResource(playerUuid, ResourceDefinitions.PEARL, pearl);
            factual(playerUuid, GameplayEventType.UNIQUE_RUNE_DUPLICATE, CollectionId.CRYSTAL_COLLECTION, rune.runeId);
            return Result.duplicate(rune.runeId, pearl);
        }
        factual(playerUuid, GameplayEventType.UNIQUE_RUNE_OBTAINED, CollectionId.CRYSTAL_COLLECTION, rune.runeId);
        return Result.grant(rune.runeId);
    }

    public synchronized Result purchaseOfferings(UUID playerUuid, long requestNonce) {
        if (!acceptNonce(playerUuid, requestNonce)) return Result.of(ResultCode.REPLAYED);
        if (!isUnlocked(playerUuid, CollectionId.OFFERINGS_COLLECTION)) return Result.of(ResultCode.LOCKED);
        PlayerState state = repository.load(playerUuid); CollectionState collection = state.collection(CollectionId.OFFERINGS_COLLECTION);
        if (collection.progress() >= UniqueRuneModel.RUNES_PER_COLLECTION) return Result.of(ResultCode.COMPLETE);
        BigDecimal cost = policy.offeringsPurchaseCost(); if (!offerings.spend(playerUuid, cost)) return Result.of(ResultCode.INSUFFICIENT_RESOURCE);
        RuneDefinition rune = roll(policy.definition(CollectionId.OFFERINGS_COLLECTION).runes);
        boolean duplicate = collection.ownsAtCurrent(rune.runeId);
        if (!duplicate) collection.owned.put(rune.runeId, collection.currentRarity);
        repository.save(playerUuid, state);
        factual(playerUuid, duplicate ? GameplayEventType.UNIQUE_RUNE_DUPLICATE : GameplayEventType.UNIQUE_RUNE_OBTAINED,
                CollectionId.OFFERINGS_COLLECTION, rune.runeId);
        return duplicate ? Result.duplicate(rune.runeId, BigDecimal.ZERO) : Result.grant(rune.runeId);
    }

    public synchronized Result claimDaily(UUID playerUuid, long epochDay, long requestNonce) {
        if (!acceptNonce(playerUuid, requestNonce)) return Result.of(ResultCode.REPLAYED);
        if (!isUnlocked(playerUuid, CollectionId.DAILY_COLLECTION)) return Result.of(ResultCode.LOCKED);
        PlayerState state = repository.load(playerUuid); if (state.lastDailyClaimDay == epochDay) return Result.of(ResultCode.DAILY_ALREADY_CLAIMED);
        CollectionState collection = state.collection(CollectionId.DAILY_COLLECTION);
        List<RuneDefinition> eligible = new ArrayList<>();
        for (RuneDefinition rune : policy.definition(CollectionId.DAILY_COLLECTION).runes) if (!collection.ownsAtCurrent(rune.runeId)) eligible.add(rune);
        if (eligible.isEmpty()) return Result.of(ResultCode.COMPLETE);
        RuneDefinition rune = roll(eligible); collection.owned.put(rune.runeId, collection.currentRarity); state.lastDailyClaimDay = epochDay;
        repository.save(playerUuid, state);
        resources.grantEarnedPlayerResource(playerUuid, policy.dailyExtraResource(), policy.dailyExtraAmount());
        factual(playerUuid, GameplayEventType.UNIQUE_RUNE_OBTAINED, CollectionId.DAILY_COLLECTION, rune.runeId);
        return Result.grant(rune.runeId);
    }

    public synchronized Result enhance(UUID playerUuid, String runeId, long requestNonce) {
        if (!acceptNonce(playerUuid, requestNonce)) return Result.of(ResultCode.REPLAYED);
        if (!isUnlocked(playerUuid, CollectionId.CRYSTAL_COLLECTION)) return Result.of(ResultCode.LOCKED);
        CollectionDefinition definition = policy.definition(CollectionId.CRYSTAL_COLLECTION); if (definition.rune(runeId) == null) return Result.of(ResultCode.INVALID);
        PlayerState state = repository.load(playerUuid); CollectionState collection = state.collection(CollectionId.CRYSTAL_COLLECTION);
        if (!collection.owned.containsKey(runeId)) return Result.of(ResultCode.INVALID);
        int level = collection.enhancement(runeId); BigDecimal cost = policy.enhancementPearlCost(runeId, level);
        if (!resources.spendPlayerResource(playerUuid, ResourceDefinitions.PEARL, cost)) return Result.of(ResultCode.INSUFFICIENT_RESOURCE);
        collection.enhancement.put(runeId, level + 1); repository.save(playerUuid, state);
        factual(playerUuid, GameplayEventType.UNIQUE_RUNE_ENHANCED, CollectionId.CRYSTAL_COLLECTION, runeId); return Result.grant(runeId);
    }

    public synchronized Result awaken(UUID playerUuid, CollectionId id, String promotedRuneId, long requestNonce) {
        if (!acceptNonce(playerUuid, requestNonce)) return Result.of(ResultCode.REPLAYED);
        if (!isUnlocked(playerUuid, id)) return Result.of(ResultCode.LOCKED);
        CollectionDefinition definition = policy.definition(id); if (definition == null || definition.rune(promotedRuneId) == null) return Result.of(ResultCode.INVALID);
        PlayerState state = repository.load(playerUuid); CollectionState collection = state.collection(id);
        if (collection.progress() < UniqueRuneModel.RUNES_PER_COLLECTION) return Result.of(ResultCode.NOT_READY);
        Rarity next = collection.currentRarity.nextGameplay(); if (next == collection.currentRarity) return Result.of(ResultCode.MAX_RARITY);
        if (!collection.ownsAtCurrent(promotedRuneId)) return Result.of(ResultCode.INVALID);
        collection.rollbackRarity = collection.currentRarity; collection.rollbackOwned.clear(); collection.rollbackOwned.putAll(collection.owned);
        collection.owned.clear(); collection.currentRarity = next; collection.owned.put(promotedRuneId, next);
        repository.save(playerUuid, state); factual(playerUuid, GameplayEventType.UNIQUE_COLLECTION_AWAKENED, id, promotedRuneId); return Result.grant(promotedRuneId);
    }

    public synchronized Result rollback(UUID playerUuid, CollectionId id, boolean confirmed, long requestNonce) {
        if (!acceptNonce(playerUuid, requestNonce)) return Result.of(ResultCode.REPLAYED);
        if (!confirmed) return Result.of(ResultCode.CONFIRMATION_REQUIRED);
        if (!isUnlocked(playerUuid, id)) return Result.of(ResultCode.LOCKED);
        PlayerState state = repository.load(playerUuid); CollectionState collection = state.collection(id);
        if (collection.rollbackRarity == null) return Result.of(ResultCode.NO_ROLLBACK);
        BigDecimal cost = policy.rollbackCrystalCost(id); if (!resources.spendPlayerResource(playerUuid, "crystals", cost)) return Result.of(ResultCode.INSUFFICIENT_RESOURCE);
        collection.currentRarity = collection.rollbackRarity; collection.owned.clear(); collection.owned.putAll(collection.rollbackOwned);
        collection.rollbackRarity = null; collection.rollbackOwned.clear(); repository.save(playerUuid, state);
        factual(playerUuid, GameplayEventType.UNIQUE_COLLECTION_ROLLED_BACK, id, ""); return Result.of(ResultCode.OK);
    }

    public SharePayload share(UUID playerUuid, CollectionId id, String runeId) {
        if (playerUuid == null || id == null || runeId == null) return null;
        PlayerState state = repository.load(playerUuid); CollectionState collection = state.collection(id); Rarity rarity = collection.owned.get(runeId);
        RuneDefinition rune = policy.definition(id).rune(runeId); if (rarity == null || rune == null) return null;
        Map<String, String> effects = new HashMap<>(); int enhancement = id == CollectionId.CRYSTAL_COLLECTION ? collection.enhancement(runeId) : 0;
        for (UniqueRuneModel.Effect effect : rune.effects) if (effect.stat != null)
            effects.put(effect.stat.name(), effect.value(rarity, enhancement).toPlainString());
        return new SharePayload(runeId, id, rarity, enhancement, effects);
    }

    public BigDecimal offeringsBalance(UUID playerUuid) { return offerings.balance(playerUuid); }
    public BigDecimal pearlBalance(UUID playerUuid) { return resources.getPlayerResourceValue(playerUuid, ResourceDefinitions.PEARL); }
    public BigDecimal crystalBalance(UUID playerUuid) { return resources.getPlayerResourceValue(playerUuid, "crystals"); }

    private RuneDefinition roll(List<RuneDefinition> pool) { return pool.get(random.nextInt(pool.size())); }
    private boolean acceptNonce(UUID playerUuid, long nonce) {
        if (playerUuid == null || nonce <= 0L) return false; Long previous = lastRequestNonce.get(playerUuid);
        if (previous != null && nonce <= previous) return false; lastRequestNonce.put(playerUuid, nonce); return true;
    }
    public void clearReplayState() { lastRequestNonce.clear(); }
    private void factual(UUID playerUuid, GameplayEventType type, CollectionId id, String runeId) {
        Map<String, String> metadata = new HashMap<>(); metadata.put("collection", id.name()); if (runeId != null && !runeId.isEmpty()) metadata.put("runeId", runeId);
        gameplayEvents.factualEvent(new GameplayEvent(playerUuid, type, BigDecimal.ONE, metadata, "unique_rune"));
    }
}
