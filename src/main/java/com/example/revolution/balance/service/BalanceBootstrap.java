package com.example.revolution.balance.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.definition.BalanceConfig;
import com.example.revolution.balance.definition.BalanceCurveDefinition;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Creates the runtime BalanceService from the bundled canonical baseline plus version overlays. */
public final class BalanceBootstrap {
    private static final String DEFAULT_RESOURCE = "/balance/default-balance.json";
    private static final String V230_RUNE_RESOURCE = "/balance/v2.3.0-rune-collections.json";

    private BalanceBootstrap() {}

    public static BalanceService activateBundledDefaults() {
        BalanceConfigParser parser = new BalanceConfigParser();
        BalanceConfigValidator validator = new BalanceConfigValidator();
        BalanceConfigCodec codec = new BalanceConfigCodec();
        BalanceService service = new BalanceService(
                parser,
                validator,
                codec,
                new BalanceCurveEngine());

        BalanceConfig baseline = parser.parse(readResource(DEFAULT_RESOURCE));
        BalanceConfig runeOverlay = parser.parse(readResource(V230_RUNE_RESOURCE));
        BalanceConfig merged = merge(baseline, runeOverlay);
        service.activate(codec.canonicalJson(merged));
        return service;
    }

    static BalanceConfig merge(BalanceConfig baseline, BalanceConfig overlay) {
        if (baseline == null || overlay == null) throw new IllegalArgumentException("balance configs are required");
        if (baseline.getBalanceSchemaVersion() != overlay.getBalanceSchemaVersion()) {
            throw new IllegalStateException("balance overlay schema version mismatch");
        }

        Map<String, String> values = new LinkedHashMap<>(baseline.getValues());
        values.putAll(overlay.getValues());
        Map<String, BalanceCurveDefinition> curves = new LinkedHashMap<>(baseline.getCurves());
        curves.putAll(overlay.getCurves());
        return new BalanceConfig(baseline.getBalanceSchemaVersion(), values, curves);
    }

    static String readResource(String path) {
        try (InputStream input = BalanceBootstrap.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("balance resource is missing: " + path);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("failed to read balance resource: " + path, e);
        }
    }
}
