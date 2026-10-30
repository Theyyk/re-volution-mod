package com.example.revolution.pet.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable Pet catalog entry. Economy/effect values are resolved from Balance Core. */
public final class PetDefinition {
    public final String petId;
    public final String technicalName;
    public final String purchaseCostValueId;
    public final List<PetEffectDefinition> effects;

    public PetDefinition(String petId, String technicalName, String purchaseCostValueId, List<PetEffectDefinition> effects) {
        if (petId == null || petId.trim().isEmpty()) throw new IllegalArgumentException("petId is required");
        if (technicalName == null || technicalName.trim().isEmpty()) throw new IllegalArgumentException("technicalName is required");
        if (purchaseCostValueId == null || purchaseCostValueId.trim().isEmpty()) throw new IllegalArgumentException("purchase cost key is required");
        this.petId = petId;
        this.technicalName = technicalName;
        this.purchaseCostValueId = purchaseCostValueId;
        this.effects = Collections.unmodifiableList(new ArrayList<>(effects == null ? Collections.<PetEffectDefinition>emptyList() : effects));
    }
}
