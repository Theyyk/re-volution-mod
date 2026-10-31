package com.example.revolution.prestige.exchange.definition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable server-owned Run Exchange catalog. */
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

    public static List<String> baselineIds() { return BASELINE_IDS; }
    public static RunExchangeCatalog empty() { return new RunExchangeCatalog(Collections.<RunExchangeOffer>emptyList()); }
    public RunExchangeOffer get(String offerId) { return offers.get(offerId); }
    public List<RunExchangeOffer> all() { return Collections.unmodifiableList(new ArrayList<>(offers.values())); }
}
