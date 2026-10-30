package com.example.revolution.pet.definition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** v2.4.0 technical catalog. Final names/art and final values remain outside this version. */
public final class PetCatalog {
    private final Map<String, PetDefinition> pets = new LinkedHashMap<>();

    public PetCatalog() {
        add(pet("pet_01", PetEffectType.DAMAGE_MULTIPLIER, "CLICK"));
        add(pet("pet_02", PetEffectType.DAMAGE_MULTIPLIER, "POISON"));
        add(pet("pet_03", PetEffectType.DAMAGE_MULTIPLIER, "BURNING"));
        add(pet("pet_04", PetEffectType.DAMAGE_MULTIPLIER, "BLEEDING"));
        add(pet("pet_05", PetEffectType.DAMAGE_MULTIPLIER, "CURSE"));
        add(pet("pet_06", PetEffectType.ALL_DAMAGE_MULTIPLIER, ""));
        add(pet("pet_07", PetEffectType.MONSTER_GOLD_MULTIPLIER, ""));
        add(pet("pet_08", PetEffectType.MONSTER_RESOURCE_MULTIPLIER, ""));
        add(pet("pet_09", PetEffectType.PET_XP_MULTIPLIER, ""));
        add(new PetDefinition("pet_10", "pet_10", "pet.purchase.pet_10", Arrays.asList(
                effect("pet_10", "critChance", PetEffectType.CLICK_CRIT_CHANCE, ""),
                effect("pet_10", "critDamage", PetEffectType.CLICK_CRIT_DAMAGE_MULTIPLIER, ""))));
        add(pet("pet_11", PetEffectType.AUTO_CLICK, ""));
        add(new PetDefinition("pet_12", "pet_12", "pet.purchase.pet_12", Arrays.asList(
                effect("pet_12", "allDamage", PetEffectType.ALL_DAMAGE_MULTIPLIER, ""),
                effect("pet_12", "goldTradeoff", PetEffectType.MONSTER_GOLD_MULTIPLIER, ""))));
        add(new PetDefinition("pet_13", "pet_13", "pet.purchase.pet_13", Arrays.asList(
                effect("pet_13", "clickSuppress", PetEffectType.DAMAGE_MULTIPLIER, "CLICK"),
                effect("pet_13", "poisonBoost", PetEffectType.DAMAGE_MULTIPLIER, "POISON"))));
        add(pet("pet_14", PetEffectType.SPECIAL_EFFECT_MULTIPLIER, ""));
        add(pet("pet_15", PetEffectType.FORGE_RESOURCE_MULTIPLIER, ""));
        add(pet("pet_16", PetEffectType.FORGE_SPEED_MULTIPLIER, ""));
        add(new PetDefinition("pet_17", "pet_17", "pet.purchase.pet_17", Arrays.asList(
                effect("pet_17", "wither", PetEffectType.DAMAGE_MULTIPLIER, "WITHER"),
                effect("pet_17", "decay", PetEffectType.DAMAGE_MULTIPLIER, "DECAY"))));
        add(new PetDefinition("pet_18", "pet_18", "pet.purchase.pet_18", Arrays.asList(
                effect("pet_18", "vulnerability", PetEffectType.VULNERABILITY, ""),
                effect("pet_18", "effectiveness", PetEffectType.EFFECTIVENESS, ""))));
    }

    private static PetDefinition pet(String id, PetEffectType type, String channel) {
        return new PetDefinition(id, id, "pet.purchase." + id,
                Collections.singletonList(effect(id, "main", type, channel)));
    }

    private static PetEffectDefinition effect(String id, String suffix, PetEffectType type, String channel) {
        return new PetEffectDefinition(type, channel, "pet.effect." + id + "." + suffix);
    }

    private void add(PetDefinition pet) {
        if (pets.put(pet.petId, pet) != null) throw new IllegalStateException("duplicate petId " + pet.petId);
    }

    public PetDefinition get(String petId) { return pets.get(petId); }
    public List<PetDefinition> all() { return Collections.unmodifiableList(new ArrayList<>(pets.values())); }
    public boolean contains(String petId) { return pets.containsKey(petId); }
}
