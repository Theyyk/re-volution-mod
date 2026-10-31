package com.example.revolution.prestige.exchange.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.pet.service.PetService;
import com.example.revolution.pet.state.PetPlayerState;
import com.example.revolution.prestige.PrestigeService;
import com.example.revolution.prestige.PrestigeState;
import com.example.revolution.prestige.exchange.definition.RunExchangeCatalog;
import com.example.revolution.prestige.exchange.definition.RunExchangeOffer;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import java.math.BigDecimal;
import java.util.UUID;

/** Server-authoritative v2.6.0 Run Exchange. Client input is only offerId + expectedRunId. */
public final class RunExchangeService {
    public enum Result {
        APPLIED, UNKNOWN_OFFER, DISABLED, LOCKED, NO_ACTIVE_RUN, STALE_RUN,
        INSUFFICIENT_FRAGMENTS, INSUFFICIENT_CRYSTALS, SLOT_LIMIT, INVALID_TARGET
    }

    public static final class Preview {
        public final String offerId;
        public final RunExchangeOffer.PaymentCurrency paymentCurrency;
        public final RunExchangeOffer.TargetKind targetKind;
        public final String targetResource;
        public final int purchaseCount;
        public final BigDecimal currentCost;
        public final BigDecimal currentTargetBalance;
        public final BigDecimal rewardPercent;
        public final BigDecimal expectedReward;

        private Preview(RunExchangeOffer offer, int purchaseCount, BigDecimal currentCost,
                        BigDecimal currentTargetBalance, BigDecimal rewardPercent, BigDecimal expectedReward) {
            this.offerId = offer.offerId;
            this.paymentCurrency = offer.paymentCurrency;
            this.targetKind = offer.targetKind;
            this.targetResource = offer.targetResource;
            this.purchaseCount = purchaseCount;
            this.currentCost = currentCost;
            this.currentTargetBalance = currentTargetBalance;
            this.rewardPercent = rewardPercent;
            this.expectedReward = expectedReward;
        }

        public int purchaseNumber() { return purchaseCount == Integer.MAX_VALUE ? Integer.MAX_VALUE : purchaseCount + 1; }
    }

    private final PrestigeService prestige;
    private final ResourceService resources;
    private final PetService pets;
    private final BalanceService balance;
    private final RunExchangeCatalog catalog;

    public RunExchangeService(PrestigeService prestige, ResourceService resources, PetService pets,
                              BalanceService balance, RunExchangeCatalog catalog) {
        if (prestige == null || resources == null || pets == null || balance == null || catalog == null) {
            throw new IllegalArgumentException("Run Exchange dependencies are required");
        }
        this.prestige = prestige;
        this.resources = resources;
        this.pets = pets;
        this.balance = balance;
        this.catalog = catalog;
    }

    public synchronized Result exchange(UUID playerUuid, String expectedRunId, String offerId) {
        RunExchangeOffer offer = catalog.get(offerId);
        if (offer == null) return Result.UNKNOWN_OFFER;
        if (!offer.enabled) return Result.DISABLED;
        if (!unlocked(offer)) return Result.LOCKED;
        PrestigeState.RunState run = prestige.activeRun(playerUuid);
        if (run == null) return Result.NO_ACTIVE_RUN;
        if (expectedRunId == null || !expectedRunId.equals(run.runId)) return Result.STALE_RUN;

        if (offer.isPetSlotOffer()) return purchasePetSlot(playerUuid, expectedRunId);
        String target = ResourceDefinitions.findCanonicalName(offer.targetResource);
        if (!approvedTarget(target)) return Result.INVALID_TARGET;

        int purchaseCount = count(run, offer.offerId);
        BigDecimal cost = cost(offer, purchaseCount);
        BigDecimal rewardPercent = rewardPercent(offer, purchaseCount);
        BigDecimal factualTargetBalance = resources.getPlayerResourceValue(playerUuid, target).max(BigDecimal.ZERO);
        BigDecimal reward = percentageOf(factualTargetBalance, rewardPercent);

        Result charge = charge(playerUuid, expectedRunId, run, offer.paymentCurrency, cost);
        if (charge != Result.APPLIED) return charge;
        if (!resources.grantPlayerResource(playerUuid, target, reward)) {
            throw new IllegalStateException("Validated Run Exchange target could not be granted");
        }
        if (!prestige.recordRunExchangePurchase(playerUuid, expectedRunId, offer.offerId, purchaseCount)) {
            throw new IllegalStateException("Run Exchange purchase counter changed during authoritative purchase");
        }
        return Result.APPLIED;
    }

    /** Read-only data for a functional UI/debug surface. Purchase always recalculates these values. */
    public synchronized Preview preview(UUID playerUuid, String expectedRunId, String offerId) {
        RunExchangeOffer offer = catalog.get(offerId);
        if (offer == null || !offer.enabled || !unlocked(offer)) return null;
        PrestigeState.RunState run = prestige.activeRun(playerUuid);
        if (run == null || expectedRunId == null || !expectedRunId.equals(run.runId)) return null;
        if (offer.isPetSlotOffer()) {
            PetPlayerState state = pets.snapshot(playerUuid);
            int purchases = state.run == null ? 0 : boundedCount(state.run.totalNormalSlotPurchasesThisRun);
            return new Preview(offer, purchases, pets.nextNormalSlotCost(playerUuid),
                    BigDecimal.valueOf(state.run == null ? 0 : state.run.normalSlotsUnlockedThisRun),
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }
        String target = ResourceDefinitions.findCanonicalName(offer.targetResource);
        if (!approvedTarget(target)) return null;
        int purchaseCount = count(run, offer.offerId);
        BigDecimal factual = resources.getPlayerResourceValue(playerUuid, target).max(BigDecimal.ZERO);
        BigDecimal percent = rewardPercent(offer, purchaseCount);
        return new Preview(offer, purchaseCount, cost(offer, purchaseCount), factual, percent, percentageOf(factual, percent));
    }

    private Result purchasePetSlot(UUID playerUuid, String expectedRunId) {
        PrestigeState.RunState current = prestige.activeRun(playerUuid);
        if (current == null) return Result.NO_ACTIVE_RUN;
        if (!expectedRunId.equals(current.runId)) return Result.STALE_RUN;
        PetService.Result result = pets.purchaseNormalSlot(playerUuid);
        if (result == PetService.Result.APPLIED) return Result.APPLIED;
        if (result == PetService.Result.NO_ACTIVE_RUN) return Result.NO_ACTIVE_RUN;
        if (result == PetService.Result.SLOT_LIMIT) return Result.SLOT_LIMIT;
        if (result == PetService.Result.INSUFFICIENT_RESOURCE) return Result.INSUFFICIENT_FRAGMENTS;
        return Result.LOCKED;
    }

    private Result charge(UUID playerUuid, String expectedRunId, PrestigeState.RunState run,
                          RunExchangeOffer.PaymentCurrency currency, BigDecimal cost) {
        if (currency == RunExchangeOffer.PaymentCurrency.FRAGMENTS) {
            if (run.runBranchCurrency.compareTo(cost) < 0) return Result.INSUFFICIENT_FRAGMENTS;
            return prestige.spendRunBranchCurrency(playerUuid, expectedRunId, cost) ? Result.APPLIED : Result.STALE_RUN;
        }
        if (currency == RunExchangeOffer.PaymentCurrency.CRYSTALS) {
            if (!resources.canAfford(playerUuid, "crystals", cost)) return Result.INSUFFICIENT_CRYSTALS;
            return resources.spendPlayerResource(playerUuid, "crystals", cost) ? Result.APPLIED : Result.INSUFFICIENT_CRYSTALS;
        }
        return Result.LOCKED;
    }

    private BigDecimal cost(RunExchangeOffer offer, int purchaseCount) {
        BigDecimal value = balance.evaluateActiveCurve(offer.costCurveId, purchaseCount);
        if (value == null || value.signum() <= 0) throw new IllegalStateException("Run Exchange cost must be positive for " + offer.offerId);
        return value.stripTrailingZeros();
    }

    private BigDecimal rewardPercent(RunExchangeOffer offer, int purchaseCount) {
        BigDecimal value = balance.evaluateActiveCurve(offer.rewardPercentCurveId, purchaseCount);
        if (value == null || value.signum() < 0) throw new IllegalStateException("Run Exchange reward percent must be non-negative for " + offer.offerId);
        return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
    }

    private boolean unlocked(RunExchangeOffer offer) {
        return "mechanic_available".equalsIgnoreCase(offer.unlockPolicy) || "always".equalsIgnoreCase(offer.unlockPolicy);
    }

    private static boolean approvedTarget(String target) {
        return "coins".equals(target) || ResourceDefinitions.RUNE_DUST.equals(target) || ResourceDefinitions.RUNE_SHARDS.equals(target);
    }

    private static int count(PrestigeState.RunState run, String offerId) {
        Integer value = run.runExchangePurchaseCounts.get(offerId);
        return value == null ? 0 : Math.max(0, value);
    }

    private static int boundedCount(long value) {
        if (value <= 0L) return 0;
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static BigDecimal percentageOf(BigDecimal factualBalance, BigDecimal percent) {
        if (factualBalance == null || factualBalance.signum() <= 0 || percent == null || percent.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal reward = factualBalance.multiply(percent).movePointLeft(2);
        return reward.signum() == 0 ? BigDecimal.ZERO : reward.stripTrailingZeros();
    }

    public RunExchangeCatalog catalog() { return catalog; }
}
