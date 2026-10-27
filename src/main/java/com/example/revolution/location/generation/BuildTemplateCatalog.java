package com.example.revolution.location.generation;

/** Resolves an existing construction-mod template, including format/block validation. */
public interface BuildTemplateCatalog {
    void validate(String templateName);
}
