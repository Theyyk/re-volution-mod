package com.example.revolution.client.mob;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Client-only palette presets for the complete Location 1 visual roster. */
public final class ForestMobPaletteCatalog {
    public static final class Palette {
        public final int base;
        public final int shadow;
        public final int accent;
        public final int detail;

        private Palette(int base, int shadow, int accent, int detail) {
            this.base = 0xFF000000 | base;
            this.shadow = 0xFF000000 | shadow;
            this.accent = 0xFF000000 | accent;
            this.detail = 0xFF000000 | detail;
        }
    }

    private static final Map<String, Palette> BY_APPEARANCE;

    static {
        Map<String, Palette> map = new LinkedHashMap<>();
        add(map, "moss_turtle",        0x607A48, 0x394B2C, 0x88A95D, 0x7B6544);
        add(map, "thorn_turtle",       0x4C6841, 0x2D3F29, 0x8D7A43, 0x6C4A36);
        add(map, "root_turtle",        0x6E6A48, 0x413B2C, 0x7D8A50, 0x80603B);
        add(map, "bloom_turtle",       0x63834E, 0x3B5132, 0xC98BA7, 0xE6D789);
        add(map, "ancient_turtle",     0x505E43, 0x2F392A, 0x71804D, 0x6A563C);

        add(map, "forest_snake",       0x547B45, 0x30482B, 0x8AAE5B, 0xD2C06F);
        add(map, "root_serpent",       0x6E6042, 0x403524, 0x75864D, 0x9D7B4B);
        add(map, "thorn_serpent",      0x405F38, 0x273A24, 0x7D8942, 0x72513A);
        add(map, "wood_serpent",       0x67533B, 0x3B2D21, 0x84905B, 0xA17D4F);
        add(map, "ancient_serpent",    0x4A513B, 0x292F25, 0x6E8051, 0xB48B55);

        add(map, "forest_mantis",      0x5D8A48, 0x34512C, 0xA2C45E, 0xD6C77B);
        add(map, "wood_mantis",        0x6C6541, 0x413A28, 0x7C9851, 0x9E7D4A);
        add(map, "thorn_mantis",       0x42613A, 0x263A25, 0x879546, 0x74513A);
        add(map, "emerald_mantis",     0x2E7B51, 0x19452F, 0x63C784, 0xD4D56D);
        add(map, "ancient_mantis",     0x435846, 0x253329, 0x75905D, 0xA98755);

        add(map, "mushroomer",         0x7B6B54, 0x4A3C31, 0xA97862, 0xD1BE89);
        add(map, "mushroom_crawler",   0x78614C, 0x493A2E, 0xA85F58, 0xCBAA73);
        add(map, "sporeling",          0x69604B, 0x3D372B, 0xB58B5D, 0xD8C598);
        add(map, "mushroom_guardian",  0x564E43, 0x302B26, 0x87504A, 0xB89365);
        add(map, "ancient_mycelium",   0x4B463D, 0x292621, 0x7D6950, 0xA78B63);

        add(map, "small_wood_golem",   0x70583D, 0x423221, 0x71834A, 0x94724A);
        add(map, "wood_golem",         0x685038, 0x3D2F20, 0x6B7F47, 0x8A6844);
        add(map, "root_golem",         0x624933, 0x392A1D, 0x78894E, 0x9B774B);
        add(map, "moss_golem",         0x5B5538, 0x342F20, 0x6F8C4A, 0x896B43);
        add(map, "ancient_golem",      0x4D4634, 0x2C281E, 0x657849, 0x795D40);

        add(map, "forest_wisp",        0x557F5D, 0x31503A, 0x87D58E, 0xD4F0A2);
        add(map, "grove_spirit",       0x477762, 0x29473A, 0x6BC99B, 0xC4E9A7);
        add(map, "flower_spirit",      0x5D7D63, 0x35493B, 0xD28FAE, 0xE8D98B);
        add(map, "ancient_spirit",     0x3E6657, 0x223A31, 0x70B891, 0xBCDCA0);
        add(map, "grove_keeper",       0x3B5C4A, 0x21342A, 0x79B87D, 0xD4C77E);

        add(map, "forest_deer",        0x705D46, 0x43372A, 0x6F8A50, 0xB89A66);
        add(map, "thorn_deer",         0x62543E, 0x3A3125, 0x718147, 0x8B6444);
        add(map, "moss_deer",          0x5C5A3E, 0x353425, 0x6E8C4C, 0x957649);
        add(map, "ancient_deer",       0x514A39, 0x2F2B22, 0x70814B, 0xA18355);
        add(map, "thicket_keeper",     0x45463A, 0x282921, 0x67804E, 0x8E704B);

        add(map, "forest_dryad",       0x6A7450, 0x3D4732, 0x7FA75B, 0xC49B69);
        add(map, "thorn_dryad",        0x596348, 0x343B2D, 0x7E874B, 0x855B43);
        add(map, "flower_dryad",       0x687452, 0x3B4532, 0xCF8EAD, 0xE2CF82);
        add(map, "grove_guardian",     0x536146, 0x30382A, 0x758B52, 0xA87D54);
        add(map, "ancient_dryad",      0x48533F, 0x293126, 0x677A4B, 0x8E704C);

        add(map, "ancient_forest_guardian", 0x454B38, 0x262A21, 0x6E854B, 0x8B6744);
        BY_APPEARANCE = Collections.unmodifiableMap(map);
    }

    private ForestMobPaletteCatalog() {}

    private static void add(Map<String, Palette> map, String id, int base, int shadow, int accent, int detail) {
        map.put(id, new Palette(base, shadow, accent, detail));
    }

    public static Palette get(String appearanceId) {
        Palette palette = BY_APPEARANCE.get(appearanceId);
        if (palette != null) return palette;
        return new Palette(0x777777, 0x444444, 0x99AA77, 0xAA8866);
    }

    public static int size() { return BY_APPEARANCE.size(); }
}
