package com.example.revolution.prestige.exchange.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable server-owned Run Exchange catalog. Concrete offers require a separate product decision. */
public final class RunExchangeCatalog {
    private final Map<String, RunExchangeOffer> offers;

    public RunExchangeCatalog(List<RunExchangeOffer> definitions) {
        Map<String, RunExchangeOffer> built = new LinkedHashMap<>();
        if (definitions != null) for (RunExchangeOffer offer : definitions) {
            if (offer == null) continue;
            if (built.put(offer.offerId, offer) != null) throw new IllegalArgumentException("duplicate Run Exchange offer " + offer.offerId);
        }
        offers = Collections.unmodifiableMap(built);
    }

    public static RunExchangeCatalog empty() { return new RunExchangeCatalog(Collections.<RunExchangeOffer>emptyList()); }
    public RunExchangeOffer get(String offerId) { return offers.get(offerId); }
    public List<RunExchangeOffer> all() { return Collections.unmodifiableList(new ArrayList<>(offers.values())); }
}
