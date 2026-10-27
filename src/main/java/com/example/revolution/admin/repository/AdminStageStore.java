package com.example.revolution.admin.repository;

import com.example.revolution.admin.definition.StagePatch;
import java.util.List;

/** Persistence-specific journal and document handling stay behind this typed port. */
public interface AdminStageStore {
    String revision();
    void recover();
    void commit(String expectedRevision, List<StagePatch> patches);
}
