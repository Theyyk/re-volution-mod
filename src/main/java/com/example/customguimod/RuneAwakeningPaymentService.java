package com.example.customguimod;

import net.minecraft.entity.player.EntityPlayerMP;

final class RuneAwakeningPaymentService {
    private RuneAwakeningPaymentService() {}

    static Result charge(EntityPlayerMP player, int statusId, int currentRank) {
        if (!RuneEconomyConfig.isAwakeningPaymentConfigured(statusId, currentRank)) {
            return Result.free();
        }

        String configuredResource = RuneEconomyConfig.awakeningResourceForStatus(statusId);
        String canonicalResource = ResourceManager.findCanonicalName(configuredResource);
        int cost = RuneEconomyConfig.awakeningCost(statusId, currentRank);

        if (canonicalResource == null || canonicalResource.trim().isEmpty() || cost <= 0) {
            return Result.invalid(configuredResource, cost);
        }

        int balance = ResourceManager.getPlayerResource(player.getUniqueID(), canonicalResource);
        if (balance < cost) {
            return Result.insufficient(canonicalResource, cost, balance);
        }

        int newBalance = ResourceManager.addPlayerResource(
                player.getUniqueID(), canonicalResource, -cost);
        return Result.paid(canonicalResource, cost, newBalance);
    }

    static final class Result {
        final boolean allowed;
        final boolean paid;
        final boolean invalidConfiguration;
        final String resource;
        final int cost;
        final int balance;

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
