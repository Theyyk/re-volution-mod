package com.example.revolution.location.repository;

import com.example.revolution.location.definition.LocationStageTemplate;

/** Persistence port for per-Location gameplay stage geometry templates. */
public interface LocationStageTemplateRepository {
    LocationStageTemplate get(int worldLocationId);
    void upsert(LocationStageTemplate template);
    void delete(int worldLocationId);
}
