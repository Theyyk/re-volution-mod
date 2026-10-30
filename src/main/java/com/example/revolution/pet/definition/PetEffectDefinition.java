package com.example.revolution.pet.definition;

/** Immutable effect declaration. Values are resolved from Balance Core by key. */
public final class PetEffectDefinition {
    public final PetEffectType type;
    public final String channel;
    public final String balanceValueId;

    public PetEffectDefinition(PetEffectType type, String channel, String balanceValueId) {
        if (type == null) throw new IllegalArgumentException("pet effect type is required");
        if (balanceValueId == null || balanceValueId.trim().isEmpty()) {
            throw new IllegalArgumentException("pet effect balance value is required");
        }
        this.type = type;
        this.channel = channel == null ? "" : channel.trim().toUpperCase();
        this.balanceValueId = balanceValueId;
    }
}
