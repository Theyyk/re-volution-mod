package com.example.revolution.balance.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Creates the runtime BalanceService from the bundled canonical baseline. */
public final class BalanceBootstrap {
    private static final String DEFAULT_RESOURCE = "/balance/default-balance.json";

    private BalanceBootstrap() {}

    public static BalanceService activateBundledDefaults() {
        BalanceService service = new BalanceService(
                new BalanceConfigParser(),
                new BalanceConfigValidator(),
                new BalanceConfigCodec(),
                new BalanceCurveEngine());
        service.activate(readResource(DEFAULT_RESOURCE));
        return service;
    }

    static String readResource(String path) {
        try (InputStream input = BalanceBootstrap.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("balance resource is missing: " + path);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("failed to read balance resource: " + path, e);
        }
    }
}
