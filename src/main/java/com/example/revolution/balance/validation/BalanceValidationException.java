package com.example.revolution.balance.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Raised only after a complete semantic validation pass. */
public final class BalanceValidationException extends IllegalArgumentException {
    private final List<String> errors;

    public BalanceValidationException(List<String> errors) {
        super(buildMessage(errors));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public List<String> getErrors() {
        return errors;
    }

    private static String buildMessage(List<String> errors) {
        if (errors == null || errors.isEmpty()) return "balance config validation failed";
        return "balance config validation failed: " + String.join("; ", errors);
    }
}
