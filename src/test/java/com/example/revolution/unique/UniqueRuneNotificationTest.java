package com.example.revolution.unique;

import com.example.revolution.network.UniqueRuneUiText;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class UniqueRuneNotificationTest {
    @Test void purchaseSuccessIsFactualAndAddressedAsPlayerFeedback() {
        UniqueRuneService.Result result = UniqueRuneService.Result.grant("crystal_01");
        String message = UniqueRuneUiText.notification("purchase_crystal", result, "Кристальная руна №1", 0);
        assertEquals("Получена Кристальная руна №1", message);
    }

    @Test void duplicateReportsExactPearlCompensation() {
        UniqueRuneService.Result result = UniqueRuneService.Result.duplicate("crystal_08", new BigDecimal("12.500"));
        assertEquals("Дубликат: Кристальная руна №8. Компенсация: 12.5 жемчуга",
                UniqueRuneUiText.notification("purchase_crystal", result, "Кристальная руна №8", 0));
    }

    @Test void enhancementIsReportedOnlyAfterSuccessfulServerResult() {
        assertEquals("Усиление: Кристальная руна №3 (+5)",
                UniqueRuneUiText.notification("enhance", UniqueRuneService.Result.grant("crystal_03"),
                        "Кристальная руна №3", 5));
        assertEquals("Недостаточно ресурсов для операции с руной",
                UniqueRuneUiText.notification("enhance",
                        UniqueRuneService.Result.of(UniqueRuneService.ResultCode.INSUFFICIENT_RESOURCE),
                        "Кристальная руна №3", 0));
    }

    @Test void replayNeverGeneratesSuccessOrSpam() {
        assertNull(UniqueRuneUiText.notification("purchase_crystal",
                UniqueRuneService.Result.of(UniqueRuneService.ResultCode.REPLAYED), "Руна", 0));
        assertNull(UniqueRuneUiText.notification("enhance",
                UniqueRuneService.Result.of(UniqueRuneService.ResultCode.REPLAYED), "Руна", 0));
        assertNull(UniqueRuneUiText.notification("purchase_offerings",
                UniqueRuneService.Result.grant("offerings_01"), "Руна", 0));
    }
}
