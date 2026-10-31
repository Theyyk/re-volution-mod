package com.example.revolution.prestige.exchange.definition;

/** Immutable server-owned Run Exchange offer definition. Numeric policy lives in Balance Core. */
public final class RunExchangeOffer {
    public enum PaymentCurrency { FRAGMENTS, CRYSTALS }
    public enum TargetKind { RESOURCE, NEXT_NORMAL_PET_SLOT }

    public final String offerId;
    public final PaymentCurrency paymentCurrency;
    public final TargetKind targetKind;
    public final String targetResource;
    public final String costCurveId;
    public final String rewardPercentCurveId;
    public final String unlockPolicy;
    public final boolean enabled;

    private RunExchangeOffer(String offerId, PaymentCurrency paymentCurrency, TargetKind targetKind,
                             String targetResource, String costCurveId, String rewardPercentCurveId,
                             String unlockPolicy, boolean enabled) {
        if (blank(offerId)) throw new IllegalArgumentException("offerId is required");
        if (paymentCurrency == null) throw new IllegalArgumentException("paymentCurrency is required");
        if (targetKind == null) throw new IllegalArgumentException("targetKind is required");
        if (blank(unlockPolicy)) throw new IllegalArgumentException("unlockPolicy is required");
        if (targetKind == TargetKind.RESOURCE) {
            if (blank(targetResource)) throw new IllegalArgumentException("targetResource is required");
            if (blank(costCurveId)) throw new IllegalArgumentException("costCurveId is required");
            if (blank(rewardPercentCurveId)) throw new IllegalArgumentException("rewardPercentCurveId is required");
        } else {
            if (paymentCurrency != PaymentCurrency.FRAGMENTS) throw new IllegalArgumentException("normal Pet Slot must use Fragments");
            if (targetResource != null || costCurveId != null || rewardPercentCurveId != null) {
                throw new IllegalArgumentException("normal Pet Slot uses the canonical Pet service policies");
            }
        }
        this.offerId = offerId;
        this.paymentCurrency = paymentCurrency;
        this.targetKind = targetKind;
        this.targetResource = targetResource;
        this.costCurveId = costCurveId;
        this.rewardPercentCurveId = rewardPercentCurveId;
        this.unlockPolicy = unlockPolicy;
        this.enabled = enabled;
    }

    public static RunExchangeOffer resource(String offerId, PaymentCurrency paymentCurrency, String targetResource,
                                            String costCurveId, String rewardPercentCurveId,
                                            String unlockPolicy, boolean enabled) {
        return new RunExchangeOffer(offerId, paymentCurrency, TargetKind.RESOURCE, targetResource,
                costCurveId, rewardPercentCurveId, unlockPolicy, enabled);
    }

    public static RunExchangeOffer normalPetSlot(String offerId, String unlockPolicy, boolean enabled) {
        return new RunExchangeOffer(offerId, PaymentCurrency.FRAGMENTS, TargetKind.NEXT_NORMAL_PET_SLOT,
                null, null, null, unlockPolicy, enabled);
    }

    public boolean isResourceOffer() { return targetKind == TargetKind.RESOURCE; }
    public boolean isPetSlotOffer() { return targetKind == TargetKind.NEXT_NORMAL_PET_SLOT; }

    public static PaymentCurrency parsePaymentCurrency(String value) {
        if (value == null) throw new IllegalArgumentException("payment currency is required");
        try { return PaymentCurrency.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("unsupported Run Exchange payment currency '" + value + "'"); }
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
