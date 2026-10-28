package com.example.revolution.rune.service;

import com.example.revolution.rune.definition.RuneEnhancementTargeting;

/** Immutable result of one planned enhancement action. */
public final class RuneEnhancementPlan {
    private final String resource;
    private final int totalEnhancementLevel;
    private final int totalCost;
    private final int requestedAmount;
    private final RuneEnhancementTargeting targeting;
    private final int[] increments;

    public RuneEnhancementPlan(
            String resource,
            int totalEnhancementLevel,
            int totalCost,
            int requestedAmount,
            RuneEnhancementTargeting targeting,
            int[] increments) {
        if (resource == null || resource.trim().isEmpty()) throw new IllegalArgumentException("resource is required");
        if (totalEnhancementLevel < 0) throw new IllegalArgumentException("total enhancement level must be >= 0");
        if (totalCost <= 0) throw new IllegalArgumentException("total cost must be positive");
        if (requestedAmount <= 0) throw new IllegalArgumentException("requested amount must be positive");
        if (targeting == null) throw new IllegalArgumentException("targeting is required");
        if (increments == null) throw new IllegalArgumentException("increments are required");
        this.resource = resource;
        this.totalEnhancementLevel = totalEnhancementLevel;
        this.totalCost = totalCost;
        this.requestedAmount = requestedAmount;
        this.targeting = targeting;
        this.increments = increments.clone();
    }

    public String getResource() { return resource; }
    public int getTotalEnhancementLevel() { return totalEnhancementLevel; }
    public int getTotalCost() { return totalCost; }
    public int getRequestedAmount() { return requestedAmount; }
    public RuneEnhancementTargeting getTargeting() { return targeting; }
    public int[] getIncrements() { return increments.clone(); }

    public int getAffectedSlots() {
        int count = 0;
        for (int increment : increments) if (increment > 0) count++;
        return count;
    }
}
