package com.example.revolution.balance.validation;

import com.example.revolution.balance.definition.BalanceConfig;
import com.example.revolution.balance.service.BalanceBootstrap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class RuneV230BalanceValidatorTest {
    @Test
    void rejectsMissingFixedSlot() {
        BalanceConfig source = BalanceBootstrap.activateBundledDefaults().getActiveSnapshot().getConfig();
        Map<String, String> values = new LinkedHashMap<>(source.getValues());
        values.remove("runes.templates.technical_status.slot.25.group");
        assertRejected(source, values);
    }

    @Test
    void rejectsUnsupportedEffectTypeAndChannelTarget() {
        BalanceConfig source = BalanceBootstrap.activateBundledDefaults().getActiveSnapshot().getConfig();

        Map<String, String> badType = new LinkedHashMap<>(source.getValues());
        badType.put("runes.templates.technical_status.effect.damage_flat.1.type", "PER_COLLECTION_MAGIC");
        assertRejected(source, badType);

        Map<String, String> badTarget = new LinkedHashMap<>(source.getValues());
        badTarget.put("runes.templates.technical_status.effect.effectiveness_placeholder.1.target_selector", "NOT_A_CHANNEL");
        assertRejected(source, badTarget);
    }

    @Test
    void rejectsBrokenRankTransition() {
        BalanceConfig source = BalanceBootstrap.activateBundledDefaults().getActiveSnapshot().getConfig();
        Map<String, String> values = new LinkedHashMap<>(source.getValues());
        values.put("runes.collection_04.rankup.rank.2.target", "4");
        assertRejected(source, values);
    }

    private static void assertRejected(BalanceConfig source, Map<String, String> values) {
        BalanceConfig invalid = new BalanceConfig(source.getBalanceSchemaVersion(), values, source.getCurves());
        assertThrows(BalanceValidationException.class, () -> new BalanceConfigValidator().validate(invalid));
    }
}
