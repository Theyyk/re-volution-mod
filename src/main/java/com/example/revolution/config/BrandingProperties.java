package com.example.revolution.config;

/** Preferred v1.3 property names with a temporary fallback for pre-v1.3 launch scripts. */
public final class BrandingProperties {
    private static final String PREFIX = "revolution.";
    private static final String LEGACY_PREFIX = "customguimod.";

    private BrandingProperties() {}

    public static String get(String key) {
        return get(key, null);
    }

    public static String get(String key, String fallback) {
        String value = System.getProperty(key);
        if ((value == null || value.trim().isEmpty()) && key != null && key.startsWith(PREFIX)) {
            value = System.getProperty(LEGACY_PREFIX + key.substring(PREFIX.length()));
        }
        return value == null ? fallback : value;
    }
}
