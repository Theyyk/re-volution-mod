package com.example.revolution.pet.service;

import com.example.revolution.pet.definition.PetCatalog;
import com.example.revolution.pet.definition.PetDefinition;
import com.example.revolution.pet.definition.PetRank;
import com.example.revolution.pet.repository.PetRepository;
import com.example.revolution.pet.state.PetPlayerState;
import com.example.revolution.prestige.PrestigeService;
import com.example.revolution.prestige.PrestigeState;
import com.example.revolution.resource.service.ResourceService;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Server-authoritative Pet gameplay service. */
public final class PetService {
    public static final int NORMAL_SLOT_COUNT = 8;
    public static final int PREMIUM_SLOT_COUNT = 2;

    public enum Result {
        APPLIED, ALREADY_OWNED, UNKNOWN_PET, NO_ACTIVE_RUN, STALE_RUN, NOT_OWNED,
        INSUFFICIENT_RESOURCE, SLOT_LIMIT, NO_FREE_SLOT, SELECTED, COOLDOWN,
        BOARD_NOT_FULL, NOT_ELIGIBLE, INVALID
    }

    private final PetRepository repository;
    private final PetCatalog catalog;
    private final PetBalanceService balance;
    private final ResourceService resources;
    private final PrestigeService prestige;

    public PetService(PetRepository repository, PetCatalog catalog, PetBalanceService balance,
                      ResourceService resources, PrestigeService prestige) {
        this.repository = repository;
        this.catalog = catalog;
        this.balance = balance;
        this.resources = resources;
        this.prestige = prestige;
    }

    public synchronized PetPlayerState snapshot(UUID playerUuid) {
        RunContext ctx = run(playerUuid, true);
        PetPlayerState state = ctx == null ? repository.load(playerUuid) : ctx.state;
        removeExpiredCooldowns(state, System.currentTimeMillis());
        return state.copy();
    }

    public synchronized Result purchasePet(UUID playerUuid, String petId) {
        PetDefinition pet = catalog.get(petId);
        if (pet == null) return Result.UNKNOWN_PET;
        RunContext ctx = run(playerUuid, true);
        if (ctx == null) return Result.NO_ACTIVE_RUN;
        PetPlayerState.PetProgress progress = ctx.state.pets.get(petId);
        if ((progress != null && progress.rank.isPersistentAcrossPrestige()) || ctx.run.purchasedPetIds.contains(petId)) return Result.ALREADY_OWNED;
        if (!resources.spendPlayerResource(playerUuid, "coins", balance.purchaseCost(pet))) return Result.INSUFFICIENT_RESOURCE;
        ctx.run.purchasedPetIds.add(petId);
        ctx.state.pet(petId);
        repository.save(playerUuid, ctx.state);
        return Result.APPLIED;
    }

    public synchronized Result purchaseNormalSlot(UUID playerUuid) {
        RunContext ctx = run(playerUuid, true);
        if (ctx == null) return Result.NO_ACTIVE_RUN;
        if (ctx.run.normalSlotsUnlockedThisRun >= NORMAL_SLOT_COUNT) return Result.SLOT_LIMIT;
        long purchaseNumber = ctx.run.totalNormalSlotPurchasesThisRun + 1L;
        BigDecimal cost = balance.normalSlotCost(purchaseNumber);
        if (!prestige.spendRunBranchCurrency(playerUuid, ctx.run.runId, cost)) return Result.INSUFFICIENT_RESOURCE;
        ctx.run.normalSlotsUnlockedThisRun++;
        ctx.run.totalNormalSlotPurchasesThisRun = purchaseNumber;
        repository.save(playerUuid, ctx.state);
        return Result.APPLIED;
    }

    public synchronized Result purchasePremiumSlot(UUID playerUuid) {
        PetPlayerState state = repository.load(playerUuid);
        if (state.premiumPetSlotsUnlocked >= PREMIUM_SLOT_COUNT) return Result.SLOT_LIMIT;
        int number = state.premiumPetSlotsUnlocked + 1;
        if (!resources.spendPlayerResource(playerUuid, "crystals", balance.premiumSlotCost(number))) return Result.INSUFFICIENT_RESOURCE;
        state.premiumPetSlotsUnlocked = number;
        repository.save(playerUuid, state);
        return Result.APPLIED;
    }

    public synchronized Result selectPet(UUID playerUuid, String petId) {
        if (!catalog.contains(petId)) return Result.UNKNOWN_PET;
        RunContext ctx = run(playerUuid, true);
        if (ctx == null) return Result.NO_ACTIVE_RUN;
        removeExpiredCooldowns(ctx.state, System.currentTimeMillis());
        if (!owns(ctx.state, ctx.run, petId)) return Result.NOT_OWNED;
        if (selected(ctx.run, petId) != null) return Result.SELECTED;
        Long cooldown = ctx.run.cooldownUntil.get(petId);
        if (cooldown != null && cooldown > System.currentTimeMillis()) return Result.COOLDOWN;
        int normalUsed = 0, premiumUsed = 0;
        for (PetPlayerState.SelectedPet s : ctx.run.selectedPets) if (s.premiumSlot) premiumUsed++; else normalUsed++;
        boolean premium;
        if (normalUsed < ctx.run.normalSlotsUnlockedThisRun) premium = false;
        else if (premiumUsed < ctx.state.premiumPetSlotsUnlocked) premium = true;
        else return Result.NO_FREE_SLOT;
        ctx.run.selectedPets.add(new PetPlayerState.SelectedPet(petId, premium, nextOrder(ctx.run)));
        normalize(ctx.state, ctx.run);
        repository.save(playerUuid, ctx.state);
        return Result.APPLIED;
    }

    public synchronized Result awaken(UUID playerUuid, String petId) {
        RunContext ctx = run(playerUuid, true);
        if (ctx == null) return Result.NO_ACTIVE_RUN;
        int accessible = ctx.run.normalSlotsUnlockedThisRun + ctx.state.premiumPetSlotsUnlocked;
        if (accessible <= 0 || ctx.run.selectedPets.size() != accessible) return Result.BOARD_NOT_FULL;
        PetPlayerState.SelectedPet target = selected(ctx.run, petId);
        if (target == null) return Result.NOT_ELIGIBLE;
        PetPlayerState.PetProgress targetProgress = ctx.state.pets.get(petId);
        if (targetProgress == null || targetProgress.rank.isGameplayMaximum()) return Result.NOT_ELIGIBLE;

        PetRank sourceRank = targetProgress.rank;
        targetProgress.rank = sourceRank.nextGameplayRank();
        target.order = nextOrder(ctx.run);
        long now = System.currentTimeMillis();
        long cooldownMillis = balance.cooldownMillis();
        List<PetPlayerState.SelectedPet> preserved = new ArrayList<>();
        for (PetPlayerState.SelectedPet selected : ctx.run.selectedPets) {
            PetPlayerState.PetProgress progress = ctx.state.pets.get(selected.petId);
            if (selected.petId.equals(petId) || (progress != null && progress.rank.progressionOrder() > sourceRank.progressionOrder())) {
                preserved.add(selected);
                continue;
            }
            if (progress != null) { progress.level = BigInteger.ONE; progress.xp = BigDecimal.ZERO; }
            if (!selected.premiumSlot) ctx.run.cooldownUntil.put(selected.petId, now + cooldownMillis);
            else ctx.run.cooldownUntil.remove(selected.petId);
        }
        ctx.run.selectedPets.clear();
        ctx.run.selectedPets.addAll(preserved);
        normalize(ctx.state, ctx.run);
        repository.save(playerUuid, ctx.state);
        return Result.APPLIED;
    }

    public synchronized Result revertRank(UUID playerUuid, String petId) {
        RunContext ctx = run(playerUuid, true);
        if (ctx == null) return Result.NO_ACTIVE_RUN;
        PetPlayerState.PetProgress progress = ctx.state.pets.get(petId);
        if (progress == null || progress.rank.progressionOrder() < PetRank.ADVANCED.progressionOrder()) return Result.NOT_ELIGIBLE;
        progress.rank = progress.rank.previousGameplayRank();
        if (!progress.rank.isPersistentAcrossPrestige()) ctx.run.purchasedPetIds.add(petId);
        normalize(ctx.state, ctx.run);
        repository.save(playerUuid, ctx.state);
        return Result.APPLIED;
    }

    /** Only call for elapsed time while the player is online. No offline catch-up is performed. */
    public synchronized void accrueOnlineXp(UUID playerUuid, long elapsedMillis, BigDecimal petXpMultiplier) {
        if (elapsedMillis <= 0L) return;
        RunContext ctx = run(playerUuid, true);
        if (ctx == null || ctx.run.selectedPets.isEmpty()) return;
        BigDecimal multiplier = petXpMultiplier == null ? BigDecimal.ONE : petXpMultiplier.max(BigDecimal.ZERO);
        BigDecimal gained = balance.baseXpPerSecond().multiply(BigDecimal.valueOf(elapsedMillis))
                .divide(BigDecimal.valueOf(1000L), 12, RoundingMode.DOWN).multiply(multiplier);
        if (gained.signum() <= 0) return;
        for (PetPlayerState.SelectedPet selected : ctx.run.selectedPets) {
            PetPlayerState.PetProgress progress = ctx.state.pet(selected.petId);
            progress.xp = progress.xp.add(gained);
            int guard = 0;
            while (guard++ < 10000) {
                BigDecimal needed = balance.xpForNextLevel(progress.level);
                if (needed.signum() <= 0 || progress.xp.compareTo(needed) < 0) break;
                progress.xp = progress.xp.subtract(needed);
                progress.level = progress.level.add(BigInteger.ONE);
            }
        }
        ctx.run.lastPetXpAccrualAt = System.currentTimeMillis();
        repository.save(playerUuid, ctx.state);
    }

    /** Login/restart boundary: intentionally discards offline elapsed time for Pet XP. */
    public synchronized void markOnline(UUID playerUuid) {
        RunContext ctx = run(playerUuid, true);
        if (ctx == null) return;
        ctx.run.lastPetXpAccrualAt = System.currentTimeMillis();
        repository.save(playerUuid, ctx.state);
    }

    public synchronized List<PetDefinition> catalog() { return catalog.all(); }
    public synchronized BigDecimal nextNormalSlotCost(UUID playerUuid) {
        RunContext ctx = run(playerUuid, true);
        long purchases = ctx == null ? 0L : ctx.run.totalNormalSlotPurchasesThisRun;
        return balance.normalSlotCost(purchases + 1L);
    }

    private RunContext run(UUID playerUuid, boolean bind) {
        PrestigeState.RunState active = prestige.activeRun(playerUuid);
        if (active == null) return null;
        PetPlayerState state = repository.load(playerUuid);
        if (state.run == null) state.run = new PetPlayerState.RunState();
        if (!active.runId.equals(state.run.runId)) {
            boolean preservedFromReset = state.run.runId == null;
            List<PetPlayerState.SelectedPet> preserved = preservedFromReset ? new ArrayList<>(state.run.selectedPets) : Collections.<PetPlayerState.SelectedPet>emptyList();
            PetPlayerState.RunState next = new PetPlayerState.RunState();
            next.runId = active.runId;
            next.lastPetXpAccrualAt = System.currentTimeMillis();
            next.selectedPets.addAll(preserved);
            state.run = next;
            if (bind) repository.save(playerUuid, state);
        }
        return new RunContext(state, state.run);
    }

    private boolean owns(PetPlayerState state, PetPlayerState.RunState run, String petId) {
        PetPlayerState.PetProgress progress = state.pets.get(petId);
        return (progress != null && progress.rank.isPersistentAcrossPrestige()) || run.purchasedPetIds.contains(petId);
    }

    private void normalize(PetPlayerState state, PetPlayerState.RunState run) {
        run.selectedPets.sort(Comparator
                .<PetPlayerState.SelectedPet>comparingInt(s -> state.pet(s.petId).rank.progressionOrder()).reversed()
                .thenComparingLong(s -> s.order)
                .thenComparing(s -> s.petId));
    }

    private static PetPlayerState.SelectedPet selected(PetPlayerState.RunState run, String petId) {
        for (PetPlayerState.SelectedPet selected : run.selectedPets) if (selected.petId.equals(petId)) return selected;
        return null;
    }

    private static long nextOrder(PetPlayerState.RunState run) {
        long max = 0L;
        for (PetPlayerState.SelectedPet selected : run.selectedPets) max = Math.max(max, selected.order);
        return max == Long.MAX_VALUE ? Long.MAX_VALUE : max + 1L;
    }

    private static void removeExpiredCooldowns(PetPlayerState state, long now) {
        if (state.run == null) return;
        Iterator<java.util.Map.Entry<String, Long>> iterator = state.run.cooldownUntil.entrySet().iterator();
        while (iterator.hasNext()) if (iterator.next().getValue() <= now) iterator.remove();
    }

    private static final class RunContext {
        final PetPlayerState state;
        final PetPlayerState.RunState run;
        RunContext(PetPlayerState state, PetPlayerState.RunState run) { this.state = state; this.run = run; }
    }
}
