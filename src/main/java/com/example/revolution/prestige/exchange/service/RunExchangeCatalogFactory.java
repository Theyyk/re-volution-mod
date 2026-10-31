package com.example.revolution.prestige.exchange.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.prestige.exchange.definition.RunExchangeCatalog;
import com.example.revolution.prestige.exchange.definition.RunExchangeOffer;
import com.example.revolution.resource.definition.ResourceDefinitions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds the fixed v2.6.0 catalog structure from data-driven Balance Core policy. */
public final class RunExchangeCatalogFactory {
    private RunExchangeCatalogFactory() {}

    public static RunExchangeCatalog fromBalance(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance is required");
        List<RunExchangeOffer> definitions = new ArrayList<>();
        for (String offerId : RunExchangeCatalog.baselineIds()) definitions.add(readOffer(balance, offerId));
        return new RunExchangeCatalog(definitions);
    }

    private static RunExchangeOffer readOffer(BalanceService balance, String offerId) {
        String prefix = "run_exchange.offer." + offerId + ".";
        boolean enabled = Boolean.parseBoolean(balance.getActiveValue(prefix + "enabled"));
        RunExchangeOffer.PaymentCurrency payment = RunExchangeOffer.parsePaymentCurrency(balance.getActiveValue(prefix + "payment_currency"));
        String target = balance.getActiveValue(prefix + "target").trim().toLowerCase(Locale.ROOT);
        String unlockPolicy = balance.getActiveValue(prefix + "unlock_policy");
        if ("next_normal_pet_slot".equals(target)) {
            if (!RunExchangeCatalog.PET_SLOT_FRAGMENTS.equals(offerId)) throw new IllegalArgumentException("only the canonical Pet Slot offer may target a normal Pet Slot");
            if (payment != RunExchangeOffer.PaymentCurrency.FRAGMENTS) throw new IllegalArgumentException("normal Pet Slot offer must use Fragments");
            return RunExchangeOffer.normalPetSlot(offerId, unlockPolicy, enabled);
        }
        String canonical = ResourceDefinitions.findCanonicalName(target);
        if (!isApprovedResourceTarget(canonical)) throw new IllegalArgumentException("unsupported Run Exchange target '" + target + "'");
        return RunExchangeOffer.resource(offerId, payment, canonical,
                balance.getActiveValue(prefix + "cost_curve"),
                balance.getActiveValue(prefix + "reward_percent_curve"),
                unlockPolicy, enabled);
    }

    private static boolean isApprovedResourceTarget(String target) {
        return "coins".equals(target) || ResourceDefinitions.RUNE_DUST.equals(target) || ResourceDefinitions.RUNE_SHARDS.equals(target);
    }
}
