package com.example.revolution.admin.api;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Strict form decoder for small typed Admin API mutation requests. */
final class AdminFormData {
    private final Map<String, String> values;

    private AdminFormData(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    static AdminFormData parse(String encoded, Set<String> expectedKeys) {
        return parse(encoded, expectedKeys, 256);
    }

    static AdminFormData parse(String encoded, Set<String> expectedKeys, int maximumValueLength) {
        if (encoded == null || expectedKeys == null) throw new IllegalArgumentException("invalid form");
        Map<String, String> values = new LinkedHashMap<>();
        if (!encoded.isEmpty()) {
            String[] parts = encoded.split("&", -1);
            if (parts.length > 16) throw new IllegalArgumentException("too many form fields");
            for (String part : parts) {
                int separator = part.indexOf('=');
                if (separator <= 0) throw new IllegalArgumentException("invalid form field");
                String key = decode(part.substring(0, separator));
                String value = decode(part.substring(separator + 1));
                if (!key.matches("[A-Za-z0-9_-]{1,32}")) throw new IllegalArgumentException("invalid form key");
                if (!expectedKeys.contains(key)) throw new IllegalArgumentException("unexpected form key");
                if (value.length() > maximumValueLength) throw new IllegalArgumentException("form value too long");
                if (values.put(key, value) != null) throw new IllegalArgumentException("duplicate form key");
            }
        }
        if (!values.keySet().equals(expectedKeys)) throw new IllegalArgumentException("missing form key");
        return new AdminFormData(values);
    }

    String get(String key) { return values.get(key); }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        } catch (IllegalArgumentException malformed) {
            throw new IllegalArgumentException("invalid percent encoding");
        }
    }
}
