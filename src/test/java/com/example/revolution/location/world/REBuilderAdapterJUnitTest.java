package com.example.revolution.location.world;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

final class REBuilderAdapterJUnitTest {
    @Test void optionalConstructionModIsNotNeededToStartCoreButBuildValidationFailsClearly() {
        REBuilderAdapter adapter = new REBuilderAdapter(Paths.get("missing-config"));
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> adapter.validate("test_template"));
        assertTrue(failure.getMessage().contains("REBuilder 1.1.2 and WorldEdit 6.1.10"));
        assertNotNull(failure.getCause());
    }
}
