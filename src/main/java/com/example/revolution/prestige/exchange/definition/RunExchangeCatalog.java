package com.example.revolution.prestige.exchange.definition;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable server-owned Run Exchange catalog. Structure is fixed; balance policy is data-driven. */
public final class RunExchangeCatalog {
    public static final String PET_SLOT_FRAGMENTS = "pet_slot_fragments";
    public static final String COINS_FRAGMENTS = "coins_fragments";
    public static final String RUNE_DUST_FRAGMENTS = "rune_dust_fragments";
    public static final String SHARDS_FRAGMENTS = "shards_fragments";
    public static final String COINS_CRYSTALS = "coins_crystals";
    public static final String RUNE_DUST_CRYSTALS = "rune_dust_crystals";
    public static final String SHARDS_CRYSTALS = "shards_crystals";

    private static final List<String> BASELINE_IDS = Collections.unmodifiableList(Arrays.asList(
            PET_SLOT_FRAGMENTS,
            COINS_FRAGMENTS,
            RUNE_DUST_FRAGMENTS,
            SHARDS_FRAGMENTS,
            COINS_CRYSTALS,
            RUNE_DUST_CRYSTALS,
            SHARDS_CRYSTALS));

    private final Map<String, RunExchangeOffer> offers;

    public RunExchangeCatalog(List<RunExchangeOffer> definitions) {
        Map<String, RunExchangeOffer> built = new LinkedHashMap<>();
        if (definitions != null) for (RunExchangeOffer offer : definitions) {
            if (offer == null) continue;
            if (built.put(offer.offerId, offer) != null) throw new IllegalArgumentException("duplicate Run Exchange offer " + offer.offerId);
        }
        offers = Collections.unmodifiableMap(built);
    }

    public static RunExchangeCatalog fromBalance(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance is required");
        List<RunExchangeOffer> definitions = new ArrayList<>();
        for (String offerId : BASELINE_IDS) definitions.add(readOffer(balance, offerId));
        return new RunExchangeCatalog(definitions);
    }

    private static RunExchangeOffer readOffer(BalanceService balance, String offerId) {
        String prefix = "run_exchange.offer." + offerId + ".";
        boolean enabled = Boolean.parseBoolean(balance.getActiveValue(prefix + "enabled"));
        RunExchangeOffer.PaymentCurrency payment = RunExchangeOffer.parsePaymentCurrency(balance.getActiveValue(prefix + "payment_currency"));
        String target = balance.getActiveValue(prefix + "target").trim().toLowerCase(java.util.Locale.ROOT);
        String unlockPolicy = balance.getActiveValue(prefix + "unlock_policy");
        if ("next_normal_pet_slot".equals(target)) {
            if (!PET_SLOT_FRAGMENTS.equals(offerId)) throw new IllegalArgumentException("only the canonical Pet Slot offer may target a normal Pet Slot");
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

    public static List<String> baselineIds() { return BASELINE_IDS; }
    public static RunExchangeCatalog empty() { return new RunExchangeCatalog(Collections.<RunExchangeOffer>emptyList()); }
    public RunExchangeOffer get(String offerId) { return offers.get(offerId); }
    public List<RunExchangeOffer> all() { return Collections.unmodifiableList(new ArrayList<>(offers.values())); }
}
