package com.example.revolution.rune.service;

import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.config.RuneEconomyConfig;
import java.util.UUID;

public final class RuneAwakeningPaymentService {
    private final ResourceService resources;

    public RuneAwakeningPaymentService(ResourceService resources) {
        this.resources = resources;
    }

    public Result charge(java.util.UUID player, int statusId, int currentRank) {
        if (!RuneEconomyConfig.isAwakeningPaymentConfigured(statusId, currentRank)) {
            return Result.free();
        }

        String configuredResource = RuneEconomyConfig.awakeningResourceForStatus(statusId);
        String canonicalResource = ResourceDefinitions.findCanonicalName(configuredResource);
        int cost = RuneEconomyConfig.awakeningCost(statusId, currentRank);

        if (canonicalResource == null || canonicalResource.trim().isEmpty() || cost <= 0) {
            return Result.invalid(configuredResource, cost);
        }

        int balance = resources.getPlayerResource(player, canonicalResource);
        if (!resources.canAfford(player, canonicalResource, cost)) {
            return Result.insufficient(canonicalResource, cost, balance);
        }

        if (!resources.spendPlayerResource(player, canonicalResource, cost)) {
            int currentBalance = resources.getPlayerResource(player, canonicalResource);
            return Result.insufficient(canonicalResource, cost, currentBalance);
        }

        int newBalance = resources.getPlayerResource(player, canonicalResource);
        return Result.paid(canonicalResource, cost, newBalance);
    }

    public static final class Result {
        public final boolean allowed;
        public final boolean paid;
        public final boolean invalidConfiguration;
        public final String resource;
        public final int cost;
        public final int balance;

        private Result(boolean allowed, boolean paid, boolean invalidConfiguration,
                       String resource, int cost, int balance) {
            this.allowed = allowed;
            this.paid = paid;
            this.invalidConfiguration = invalidConfiguration;
            this.resource = resource == null ? "" : resource;
            this.cost = Math.max(0, cost);
            this.balance = balance;
        }

        static Result free() {
            return new Result(true, false, false, "", 0, -1);
        }

        static Result paid(String resource, int cost, int balance) {
            return new Result(true, true, false, resource, cost, balance);
        }

        static Result insufficient(String resource, int cost, int balance) {
            return new Result(false, false, false, resource, cost, balance);
        }

        static Result invalid(String resource, int cost) {
            return new Result(false, false, true, resource, cost, -1);
        }
    }
}
