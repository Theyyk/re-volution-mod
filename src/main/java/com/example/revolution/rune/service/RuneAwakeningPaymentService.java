package com.example.revolution.rune.service;

import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.definition.RuneRankUpRule;
import java.util.UUID;

/** Performs only the atomic resource charge for an already-resolved rank-up rule. */
public final class RuneAwakeningPaymentService {
    private final ResourceService resources;

    public RuneAwakeningPaymentService(ResourceService resources) {
        this.resources = resources;
    }

    public Result charge(UUID player, RuneRankUpRule rule) {
        if (rule == null || rule.resource == null || rule.resource.trim().isEmpty() || rule.cost <= 0) {
            return Result.invalid(rule == null ? "" : rule.resource, rule == null ? 0 : rule.cost);
        }

        int balance = resources.getPlayerResource(player, rule.resource);
        if (!resources.canAfford(player, rule.resource, rule.cost)) {
            return Result.insufficient(rule.resource, rule.cost, balance);
        }

        if (!resources.spendPlayerResource(player, rule.resource, rule.cost)) {
            int currentBalance = resources.getPlayerResource(player, rule.resource);
            return Result.insufficient(rule.resource, rule.cost, currentBalance);
        }

        int newBalance = resources.getPlayerResource(player, rule.resource);
        return Result.paid(rule.resource, rule.cost, newBalance);
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
