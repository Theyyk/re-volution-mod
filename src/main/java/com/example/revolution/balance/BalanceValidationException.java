package com.example.revolution.balance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Aggregated validation failure for a candidate balance document. */
public final class BalanceValidationException extends IllegalArgumentException {
    private final List<String> errors;

    public BalanceValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public List<String> getErrors() {
        return errors;
    }
}
