package com.example.revolution.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public final class LocationCommandTabCompletionJUnitTest {
    @Test
    void stageTemplateArgumentsExposeSequentialHints() {
        assertEquals("<mobRadius>", LocationCommand.stageTemplateArgumentHint(3));
        assertEquals("<triggerOffset>", LocationCommand.stageTemplateArgumentHint(4));
        assertEquals("<triggerRadius>", LocationCommand.stageTemplateArgumentHint(5));
    }

    @Test
    void stageTemplateHintsStopAfterFinalArgument() {
        assertNull(LocationCommand.stageTemplateArgumentHint(2));
        assertNull(LocationCommand.stageTemplateArgumentHint(6));
    }
}
