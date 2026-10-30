package com.example.revolution.prestige.exchange.definition;

import java.math.BigDecimal;

/** Server-owned Run Exchange definition. v2.4.0 intentionally ships no concrete offers. */
public final class RunExchangeOffer {
    public final String offerId;
    public final BigDecimal fragmentCost;
    public final String targetResource;
    public final BigDecimal targetAmount;

    public RunExchangeOffer(String offerId, BigDecimal fragmentCost, String targetResource, BigDecimal targetAmount) {
        if (offerId == null || offerId.trim().isEmpty()) throw new IllegalArgumentException("offerId is required");
        if (fragmentCost == null || fragmentCost.signum() <= 0) throw new IllegalArgumentException("fragmentCost must be positive");
        if (targetResource == null || targetResource.trim().isEmpty()) throw new IllegalArgumentException("targetResource is required");
        if (targetAmount == null || targetAmount.signum() <= 0) throw new IllegalArgumentException("targetAmount must be positive");
        this.offerId = offerId;
        this.fragmentCost = fragmentCost;
        this.targetResource = targetResource;
        this.targetAmount = targetAmount;
    }
}
