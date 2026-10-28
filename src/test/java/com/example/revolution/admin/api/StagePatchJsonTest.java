package com.example.revolution.admin.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StagePatchJsonTest {
    @Test void exactStringsAndAbsentFieldsRemainExact() {
        assertEquals("1.25E18", StagePatchJson.parse("[{\"location\":1,\"stage\":2,\"hp\":\"1.25E18\"}]").get(0).hp);
        assertNull(StagePatchJson.parse("[{\"location\":1,\"stage\":2,\"population\":5}]").get(0).hp);
    }
    @Test void rejectsUnknownDuplicateAndIncorrectTypes() {
        for (String input : new String[] {
                "[{\"location\":1,\"stage\":2,\"kind\":\"BOSS_ZONE\"}]",
                "[{\"location\":1,\"stage\":2,\"hp\":\"10\",\"hp\":\"20\"}]",
                "[{\"location\":1,\"stage\":2,\"hp\":1e18}]",
                "[{\"location\":1.5,\"stage\":2,\"population\":5}]",
                "[] trailing" }) assertThrows(IllegalArgumentException.class, () -> StagePatchJson.parse(input));
    }
}
