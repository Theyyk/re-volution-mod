package com.example.revolution.mob.appearance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable content/visual catalog for managed mobs. */
public final class MobAppearanceCatalog {
    private static final Map<String, MobAppearanceDefinition> BY_SLOT;
    private static final Map<String, MobAppearanceDefinition> BY_MOB_ID;
    private static final List<MobAppearanceDefinition> LOCATION_1;

    static {
        List<MobAppearanceDefinition> location1 = new ArrayList<>();
        location1.add(def(1, 1, "forest_mossback_hatchling", "Моховая черепаха", "turtle", "moss_turtle", "forest_turtle", "moss_turtle", "forest_turtle", 0.80D));
        location1.add(def(1, 2, "forest_vine_snake", "Лесная змея", "serpent", "forest_snake", "forest_serpent", "forest_snake", "forest_serpent", 0.85D));
        location1.add(def(1, 3, "forest_leaf_mantis", "Лесной богомол", "mantis", "forest_mantis", "forest_mantis", "forest_mantis", "forest_mantis", 0.85D));
        location1.add(def(1, 4, "forest_sporeling", "Грибник", "spore", "mushroomer", "forest_spore", "mushroomer", "forest_spore", 0.80D));
        location1.add(def(1, 5, "forest_twig_golem", "Малый древесный голем", "golem", "small_wood_golem", "forest_golem", "small_wood_golem", "forest_golem", 0.80D));
        location1.add(def(1, 6, "forest_wisp_seed", "Лесной огонёк", "spirit", "forest_wisp", "forest_spirit", "forest_wisp", "forest_spirit", 0.75D));
        location1.add(def(1, 7, "forest_fawn_beast", "Лесной олень", "beast", "forest_deer", "forest_stag", "forest_deer", "forest_stag", 0.85D));
        location1.add(def(1, 8, "forest_young_dryad", "Лесная дриада", "dryad", "forest_dryad", "forest_dryad", "forest_dryad", "forest_dryad", 0.90D));
        location1.add(def(1, 9, "forest_thornshell_turtle", "Колючая черепаха", "turtle", "thorn_turtle", "forest_turtle", "thorn_turtle", "forest_turtle", 0.90D));
        location1.add(def(1, 10, "forest_root_serpent", "Корневой змей", "serpent", "root_serpent", "forest_serpent", "root_serpent", "forest_serpent", 0.95D));
        location1.add(def(1, 11, "forest_bark_mantis", "Древесный богомол", "mantis", "wood_mantis", "forest_mantis", "wood_mantis", "forest_mantis", 0.95D));
        location1.add(def(1, 12, "forest_mushroom_crawler", "Грибной ползун", "spore", "mushroom_crawler", "forest_spore", "mushroom_crawler", "forest_spore", 0.95D));
        location1.add(def(1, 13, "forest_bark_golem", "Древесный голем", "golem", "wood_golem", "forest_golem", "wood_golem", "forest_golem", 0.95D));
        location1.add(def(1, 14, "forest_grove_spirit", "Дух рощи", "spirit", "grove_spirit", "forest_spirit", "grove_spirit", "forest_spirit", 0.90D));
        location1.add(def(1, 15, "forest_thornhorn", "Терновый олень", "beast", "thorn_deer", "forest_stag", "thorn_deer", "forest_stag", 0.95D));
        location1.add(def(1, 16, "forest_thorn_dryad", "Терновая дриада", "dryad", "thorn_dryad", "forest_dryad", "thorn_dryad", "forest_dryad", 0.95D));
        location1.add(def(1, 17, "forest_rootback_turtle", "Корневая черепаха", "turtle", "root_turtle", "forest_turtle", "root_turtle", "forest_turtle", 1.00D));
        location1.add(def(1, 18, "forest_thorn_serpent", "Терновый змей", "serpent", "thorn_serpent", "forest_serpent", "thorn_serpent", "forest_serpent", 1.00D));
        location1.add(def(1, 19, "forest_thorn_mantis", "Шипастый богомол", "mantis", "thorn_mantis", "forest_mantis", "thorn_mantis", "forest_mantis", 1.00D));
        location1.add(def(1, 20, "forest_puffcap_beast", "Споровик", "spore", "sporeling", "forest_spore", "sporeling", "forest_spore", 1.00D));
        location1.add(def(1, 21, "forest_root_golem", "Корневой голем", "golem", "root_golem", "forest_golem", "root_golem", "forest_golem", 1.05D));
        location1.add(def(1, 22, "forest_bloom_spirit", "Цветочный дух", "spirit", "flower_spirit", "forest_spirit", "flower_spirit", "forest_spirit", 1.00D));
        location1.add(def(1, 23, "forest_moss_stag", "Моховой олень", "beast", "moss_deer", "forest_stag", "moss_deer", "forest_stag", 1.05D));
        location1.add(def(1, 24, "forest_bloom_dryad", "Цветочная дриада", "dryad", "flower_dryad", "forest_dryad", "flower_dryad", "forest_dryad", 1.00D));
        location1.add(def(1, 25, "forest_bloomback_turtle", "Цветущая черепаха", "turtle", "bloom_turtle", "forest_turtle", "bloom_turtle", "forest_turtle", 1.10D));
        location1.add(def(1, 26, "forest_ancient_rootcoil", "Древесный змей", "serpent", "wood_serpent", "forest_serpent", "wood_serpent", "forest_serpent", 1.10D));
        location1.add(def(1, 27, "forest_emerald_reaper", "Изумрудный богомол", "mantis", "emerald_mantis", "forest_mantis", "emerald_mantis", "forest_mantis", 1.10D));
        location1.add(def(1, 28, "forest_rotcap_guardian", "Грибной страж", "spore", "mushroom_guardian", "forest_spore", "mushroom_guardian", "forest_spore", 1.10D));
        location1.add(def(1, 29, "forest_moss_golem", "Моховой голем", "golem", "moss_golem", "forest_golem", "moss_golem", "forest_golem", 1.15D));
        location1.add(def(1, 30, "forest_ancient_wisp", "Древний дух", "spirit", "ancient_spirit", "forest_spirit", "ancient_spirit", "forest_spirit", 1.10D));
        location1.add(def(1, 31, "forest_ancient_hart", "Древний олень", "beast", "ancient_deer", "forest_stag", "ancient_deer", "forest_stag", 1.15D));
        location1.add(def(1, 32, "forest_grove_valkyrie", "Страж рощи", "dryad", "grove_guardian", "forest_dryad", "grove_guardian", "forest_dryad", 1.10D));
        location1.add(def(1, 33, "forest_elder_mossback", "Древняя черепаха", "turtle", "ancient_turtle", "forest_turtle", "ancient_turtle", "forest_turtle", 1.20D));
        location1.add(def(1, 34, "forest_elder_coil", "Древний змей", "serpent", "ancient_serpent", "forest_serpent", "ancient_serpent", "forest_serpent", 1.20D));
        location1.add(def(1, 35, "forest_ancient_mantis", "Древний богомол", "mantis", "ancient_mantis", "forest_mantis", "ancient_mantis", "forest_mantis", 1.20D));
        location1.add(def(1, 36, "forest_elder_mycelium", "Древняя грибница", "spore", "ancient_mycelium", "forest_spore", "ancient_mycelium", "forest_spore", 1.20D));
        location1.add(def(1, 37, "forest_ancient_grove_golem", "Древний голем", "golem", "ancient_golem", "forest_golem", "ancient_golem", "forest_golem", 1.25D));
        location1.add(def(1, 38, "forest_elder_grove_spirit", "Хранитель рощи", "spirit", "grove_keeper", "forest_spirit", "grove_keeper", "forest_spirit", 1.20D));
        location1.add(def(1, 39, "forest_elder_forest_beast", "Хранитель чащи", "beast", "thicket_keeper", "forest_stag", "thicket_keeper", "forest_stag", 1.25D));
        location1.add(def(1, 40, "forest_elder_dryad", "Древняя дриада", "dryad", "ancient_dryad", "forest_dryad", "ancient_dryad", "forest_dryad", 1.20D));
        location1.add(def(1, 41, "forest_ancient_guardian", "Хранитель Древнего Леса", "boss", "ancient_forest_guardian", "forest_guardian_boss", "ancient_forest_guardian", "forest_guardian_boss", 1.50D));

        Map<String, MobAppearanceDefinition> bySlot = new LinkedHashMap<>();
        Map<String, MobAppearanceDefinition> byMobId = new LinkedHashMap<>();
        for (MobAppearanceDefinition definition : location1) {
            String slotKey = slotKey(definition.getLocationId(), definition.getMobSlotId());
            if (bySlot.put(slotKey, definition) != null) throw new IllegalStateException("Duplicate mob appearance slot " + slotKey);
            if (byMobId.put(definition.getMobId(), definition) != null) throw new IllegalStateException("Duplicate mobId " + definition.getMobId());
        }
        LOCATION_1 = Collections.unmodifiableList(location1);
        BY_SLOT = Collections.unmodifiableMap(bySlot);
        BY_MOB_ID = Collections.unmodifiableMap(byMobId);
    }

    private MobAppearanceCatalog() {}

    private static MobAppearanceDefinition def(
            int locationId,
            int slotId,
            String mobId,
            String displayName,
            String familyId,
            String appearanceId,
            String modelId,
            String textureId,
            String animationSetId,
            double scale) {
        return new MobAppearanceDefinition(locationId, slotId, mobId, displayName, familyId,
                appearanceId, modelId, textureId, animationSetId, scale);
    }

    private static String slotKey(int locationId, int mobSlotId) {
        return locationId + ":" + mobSlotId;
    }

    public static MobAppearanceDefinition bySlot(int locationId, int mobSlotId) {
        return BY_SLOT.get(slotKey(locationId, mobSlotId));
    }

    public static MobAppearanceDefinition byMobId(String mobId) {
        return mobId == null ? null : BY_MOB_ID.get(mobId);
    }

    public static List<MobAppearanceDefinition> location1() {
        return LOCATION_1;
    }
}
