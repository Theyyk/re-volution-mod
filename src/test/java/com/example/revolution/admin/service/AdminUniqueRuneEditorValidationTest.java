package com.example.revolution.admin.service;

import com.example.revolution.unique.UniqueRuneModel;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminUniqueRuneEditorValidationTest {
    private static final Gson JSON = new Gson();

    @Test
    void rejectsUnknownRarityBeforeMongoSave() {
        UniqueRuneModel.PlayerState state = new UniqueRuneModel.PlayerState();
        String json = JSON.toJson(state).replace("\"currentRarity\":\"COMMON\"", "\"currentRarity\":\"UNCOMMON\"");
        UniqueRuneModel.PlayerState parsed = JSON.fromJson(json, UniqueRuneModel.PlayerState.class);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AdminMechanicsService.validateUniqueRuneEditorState(parsed));
        assertTrue(error.getMessage().contains("currentRarity"));
        assertTrue(error.getMessage().contains("IMPROVED"));
    }

    @Test
    void acceptsValidImprovedRarityWithEmptyMaps() {
        UniqueRuneModel.PlayerState state = new UniqueRuneModel.PlayerState();
        state.collection(UniqueRuneModel.CollectionId.OFFERINGS_COLLECTION).currentRarity = UniqueRuneModel.Rarity.IMPROVED;
        assertDoesNotThrow(() -> AdminMechanicsService.validateUniqueRuneEditorState(state));
    }

    @Test
    void rejectsMissingCollectionAndNullOwnedRarity() {
        UniqueRuneModel.PlayerState missing = new UniqueRuneModel.PlayerState();
        missing.collections.remove(UniqueRuneModel.CollectionId.DAILY_COLLECTION);
        assertThrows(IllegalArgumentException.class,
                () -> AdminMechanicsService.validateUniqueRuneEditorState(missing));

        UniqueRuneModel.PlayerState badOwned = new UniqueRuneModel.PlayerState();
        badOwned.collection(UniqueRuneModel.CollectionId.OFFERINGS_COLLECTION).owned.put("test", null);
        assertThrows(IllegalArgumentException.class,
                () -> AdminMechanicsService.validateUniqueRuneEditorState(badOwned));
    }
}
