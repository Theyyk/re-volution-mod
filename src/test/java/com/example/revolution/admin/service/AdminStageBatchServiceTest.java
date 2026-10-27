package com.example.revolution.admin.service;

import com.example.revolution.admin.definition.AdminStageSnapshot;
import com.example.revolution.admin.definition.StagePatch;
import com.example.revolution.admin.repository.AdminStageStore;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminStageBatchServiceTest {
    static final class Store implements AdminStageStore {
        String revision = "one"; int commits;
        List<StagePatch> written;
        public String revision() { return revision; }
        public void recover() { }
        public void commit(String expected, List<StagePatch> patches) {
            if (!expected.equals(revision)) throw new IllegalArgumentException("stale");
            commits++; written = patches; revision = "two";
        }
    }
    private static AdminStageSnapshot row(int stage, boolean boss) {
        return new AdminStageSnapshot(1, stage, 100 + stage, 1, "Mob1_" + stage,
                "100", "coins", "10", boss ? "BOSS_ZONE" : "MOB_ZONE", "", boss, boss, 0, 64, 0, 10);
    }
    @Test void selectionOnlyUpdatesCheckedFieldsAndPreservesBoss() {
        Store store = new Store(); int[] reloads = {0};
        AdminStageBatchService service = new AdminStageBatchService(store, () -> Arrays.asList(row(1, false), row(2, true)), () -> reloads[0]++);
        AdminStageBatchService.Preview preview = service.preview("owner", "one", Arrays.asList(
                new StagePatch(1,1,null,5,null,null), new StagePatch(1,2,null,5,null,null)));
        assertEquals(0, store.commits); assertEquals("100", preview.after.get(0).hp);
        assertEquals("10", preview.after.get(1).amount); assertTrue(preview.after.get(1).boss);
        service.apply("owner", preview.token);
        assertEquals(1, store.commits); assertEquals(1, reloads[0]);
        assertNull(store.written.get(0).hp); assertNull(store.written.get(0).amount);
        assertThrows(IllegalArgumentException.class, () -> service.apply("owner", preview.token));
    }
    @Test void validationFailurePreventsEntireBatchAndRejectsDuplicates() {
        Store store = new Store();
        AdminStageBatchService service = new AdminStageBatchService(store, () -> Arrays.asList(row(1, false), row(2, true)), () -> {});
        assertThrows(IllegalArgumentException.class, () -> service.preview("owner", "one", Arrays.asList(
                new StagePatch(1,1,"1E18",null,null,null), new StagePatch(1,2,null,0,null,null))));
        StagePatch patch = new StagePatch(1,1,null,5,null,null);
        assertThrows(IllegalArgumentException.class, () -> service.preview("owner", "one", Arrays.asList(patch,patch)));
        assertEquals(0, store.commits);
    }
    @Test void previewsAreSessionBoundAndRevisionCheckedAtApply() {
        Store store = new Store();
        AdminStageBatchService service = new AdminStageBatchService(store, () -> Collections.singletonList(row(1, true)), () -> {});
        AdminStageBatchService.Preview preview = service.preview("first", "one", Collections.singletonList(new StagePatch(1,1,"1E18",null,null,null)));
        assertEquals("1E+18", preview.after.get(0).hp);
        assertThrows(IllegalArgumentException.class, () -> service.apply("second", preview.token));
        store.revision = "externally-changed";
        assertThrows(IllegalArgumentException.class, () -> service.apply("first", preview.token));
        assertEquals(0, store.commits);
    }
}
