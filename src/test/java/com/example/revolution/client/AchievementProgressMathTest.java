package com.example.revolution.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AchievementProgressMathTest {
    @Test void interpretsServerDecimalStrings() {
        assertEquals(0.5D, AchievementProgressMath.fraction("5", "10", false), 0.00001D);
        assertEquals(0.5D, AchievementProgressMath.fraction("5E194", "1E195", false), 0.00001D);
        assertEquals(1D, AchievementProgressMath.fraction("100", "10", false), 0.00001D);
        assertEquals(0D, AchievementProgressMath.fraction("-4", "10", false), 0.00001D);
        assertEquals(1D, AchievementProgressMath.fraction("0", "0", true), 0.00001D);
        assertEquals(0D, AchievementProgressMath.fraction("invalid", "10", false), 0.00001D);
    }
}
