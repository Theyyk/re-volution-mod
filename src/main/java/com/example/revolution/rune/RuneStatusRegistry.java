package com.example.revolution.rune;

import com.example.revolution.BrandingProperties;
/**
 * Central mapping between rune collections (decks) and status ids.
 *
 * v1.2.7 keeps Poison as the only built-in status, but future statuses can be
 * connected without changing purchase/progression/payment code by assigning a
 * positive status id to a deck with:
 * revolution.runes.deck.<deckIndex>.status=<statusId>
 *
 * Status-specific economy keys use a stable key. Built-in Poison uses
 * "poison". Other ids default to "status_<id>" and may opt into a readable
 * key with revolution.runes.status.<statusId>.key=<key>.
 */
public final class RuneStatusRegistry {
    public static final int STATUS_NONE = 0;
    public static final int STATUS_POISON = 1;

    private RuneStatusRegistry() {}

    public static int statusForCollection(int collectionIndex) {
        if (collectionIndex < 0) return STATUS_NONE;

        int fallback = collectionIndex == 0 ? STATUS_POISON : STATUS_NONE;
        String raw = BrandingProperties.get("revolution.runes.deck." + collectionIndex + ".status");
        if (raw == null || raw.trim().isEmpty()) return fallback;

        try {
            int configured = Integer.parseInt(raw.trim());
            return configured > STATUS_NONE ? configured : STATUS_NONE;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static boolean isRuneStatus(int statusId) {
        return statusId > STATUS_NONE;
    }

    public static String statusKey(int statusId) {
        if (statusId == STATUS_POISON) return "poison";
        if (!isRuneStatus(statusId)) return "none";

        String fallback = "status_" + statusId;
        String raw = BrandingProperties.get("revolution.runes.status." + statusId + ".key");
        if (raw == null) return fallback;

        String normalized = normalizeKey(raw);
        return normalized.isEmpty() ? fallback : normalized;
    }

    public static String displayNameForCollection(int collectionIndex, String storedName) {
        int statusId = statusForCollection(collectionIndex);
        if (statusId == STATUS_POISON) return "Яд";
        return storedName == null ? "" : storedName;
    }

    private static String normalizeKey(String raw) {
        String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') {
                out.append(c);
            } else if (c == '-' || Character.isWhitespace(c)) {
                if (out.length() > 0 && out.charAt(out.length() - 1) != '_') out.append('_');
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '_') {
            out.setLength(out.length() - 1);
        }
        return out.toString();
    }
}
