package com.example.revolution.location.repository;

/** One existing construction template name per world Location. */
public interface LocationBuildTemplateRepository {
    String get(int worldLocationId);
    void upsert(int worldLocationId, String templateName);
}
