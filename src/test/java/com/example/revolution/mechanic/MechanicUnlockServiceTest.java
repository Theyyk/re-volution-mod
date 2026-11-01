package com.example.revolution.mechanic;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.player.repository.PlayerIdentity;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.repository.RuneRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MechanicUnlockServiceTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000212");

    @Test void historicalMaximumCannotBeFarmedByResetAndReobtain() {
        Fixture f = new Fixture();
        f.players.setRanks(0, 3, 3);
        assertEquals(3, f.service.refreshRuneProgress(PLAYER).maxProgress(MechanicUnlockModel.Rarity.ADVANCED));
        f.players.clearCollection(PLAYER, 0);
        assertEquals(3, f.service.refreshRuneProgress(PLAYER).maxProgress(MechanicUnlockModel.Rarity.ADVANCED));
        f.players.setRanks(0, 3, 3);
        assertEquals(3, f.service.refreshRuneProgress(PLAYER).maxProgress(MechanicUnlockModel.Rarity.ADVANCED));
        f.players.setRanks(0, 3, 4);
        assertEquals(4, f.service.refreshRuneProgress(PLAYER).maxProgress(MechanicUnlockModel.Rarity.ADVANCED));
    }

    @Test void awakeningShapePreservesCommonMaxAndRaisesImprovedMax() {
        Fixture f = new Fixture();
        f.players.setRanks(0, 1, 26);
        assertEquals(26, f.service.refreshRuneProgress(PLAYER).maxProgress(MechanicUnlockModel.Rarity.COMMON));
        f.players.clearCollection(PLAYER, 0);
        f.players.addRuneToCollection(PLAYER, 0, 0, 0, 2, 1);
        MechanicUnlockModel.PlayerState state = f.service.refreshRuneProgress(PLAYER);
        assertEquals(26, state.maxProgress(MechanicUnlockModel.Rarity.COMMON));
        assertEquals(1, state.maxProgress(MechanicUnlockModel.Rarity.IMPROVED));
    }

    @Test void requirementIsEligibilityNotAutomaticUnlockAndIsNotConsumed() {
        Fixture f = new Fixture();
        f.players.setRanks(0, 2, 10);
        f.service.refreshRuneProgress(PLAYER);
        assertTrue(f.service.isEligible(PLAYER, BalanceMechanicUnlockPolicy.ARTS));
        assertFalse(f.service.isUnlocked(PLAYER, BalanceMechanicUnlockPolicy.ARTS));
        assertEquals(MechanicUnlockService.UnlockResult.OK, f.service.unlock(PLAYER, BalanceMechanicUnlockPolicy.ARTS));
        assertTrue(f.service.isUnlocked(PLAYER, BalanceMechanicUnlockPolicy.ARTS));
        assertEquals(10, f.service.state(PLAYER).maxProgress(MechanicUnlockModel.Rarity.IMPROVED));
        assertEquals(MechanicUnlockService.UnlockResult.ALREADY_UNLOCKED, f.service.unlock(PLAYER, BalanceMechanicUnlockPolicy.ARTS));
    }

    @Test void childStaysHiddenUntilParentIsFactuallyUnlocked() {
        Fixture f = new Fixture();
        f.service.setMaxProgress(PLAYER, MechanicUnlockModel.Rarity.IMPROVED, 15);
        f.service.setMaxProgress(PLAYER, MechanicUnlockModel.Rarity.ADVANCED, 10);
        assertTrue(f.service.isEligible(PLAYER, BalanceMechanicUnlockPolicy.GOD_WORSHIP));
        assertFalse(f.service.isEligible(PLAYER, BalanceMechanicUnlockPolicy.GOD_AUTO_WORSHIP));
        assertEquals(MechanicUnlockService.UnlockResult.OK, f.service.unlock(PLAYER, BalanceMechanicUnlockPolicy.GOD_WORSHIP));
        assertTrue(f.service.isEligible(PLAYER, BalanceMechanicUnlockPolicy.GOD_AUTO_WORSHIP));
    }

    @Test void forceLockBlocksAccessWithoutDestroyingNormalUnlock() {
        Fixture f = new Fixture();
        f.service.forceUnlock(PLAYER, BalanceMechanicUnlockPolicy.AURA_SYSTEM);
        assertTrue(f.service.isUnlocked(PLAYER, BalanceMechanicUnlockPolicy.AURA_SYSTEM));
        f.service.forceLock(PLAYER, BalanceMechanicUnlockPolicy.AURA_SYSTEM);
        assertFalse(f.service.isUnlocked(PLAYER, BalanceMechanicUnlockPolicy.AURA_SYSTEM));
        f.service.forceUnlock(PLAYER, BalanceMechanicUnlockPolicy.AURA_SYSTEM);
        assertTrue(f.service.isUnlocked(PLAYER, BalanceMechanicUnlockPolicy.AURA_SYSTEM));
    }

    private static final class Fixture {
        final MemPlayers players = new MemPlayers();
        final MemRepo repo = new MemRepo();
        final MechanicUnlockService service = new MechanicUnlockService(players, repo,
                new BalanceMechanicUnlockPolicy(BalanceBootstrap.activateBundledDefaults()));
    }

    private static final class MemRepo implements MechanicUnlockRepository {
        private final Map<UUID, MechanicUnlockModel.PlayerState> states = new HashMap<>();
        @Override public MechanicUnlockModel.PlayerState load(UUID uuid) {
            MechanicUnlockModel.PlayerState state = states.get(uuid); return state == null ? new MechanicUnlockModel.PlayerState() : state.copy();
        }
        @Override public void save(UUID uuid, MechanicUnlockModel.PlayerState state) { states.put(uuid, state.copy()); }
    }

    private static final class MemPlayers implements PlayerRepository {
        private final Map<Integer, List<RuneRecord>> runes = new HashMap<>();
        void setRanks(int collection, int rank, int count) {
            List<RuneRecord> values = new ArrayList<>();
            for (int i = 0; i < count; i++) values.add(new RuneRecord(i, i, rank, 1));
            runes.put(collection, values);
        }
        @Override public List<RuneRecord> getRunesInCollection(UUID uuid, int collection) { return new ArrayList<>(runes.getOrDefault(collection, new ArrayList<>())); }
        @Override public void addRuneToCollection(UUID uuid, int collection, int slot, int rune, int layer, int level) { runes.computeIfAbsent(collection, k -> new ArrayList<>()).add(new RuneRecord(slot, rune, layer, level)); }
        @Override public void clearCollection(UUID uuid, int collection) { runes.remove(collection); }
        @Override public List<PlayerIdentity> listKnownPlayers() { return new ArrayList<>(); }
        @Override public void rememberIdentity(UUID uuid, String name) {}
        @Override public int getBalance(UUID uuid) { return 0; }
        @Override public void setBalance(UUID uuid, int balance) {}
        @Override public int getCrystals(UUID uuid) { return 0; }
        @Override public void setCrystals(UUID uuid, int crystals) {}
        @Override public int getActiveCollection(UUID uuid) { return 0; }
        @Override public void setActiveCollection(UUID uuid, int index) {}
        @Override public List<String> getCollectionNames(UUID uuid) { return new ArrayList<>(); }
        @Override public void addCollection(UUID uuid, String name) {}
        @Override public void deleteCollection(UUID uuid, int index) {}
        @Override public boolean setRuneRank(UUID uuid, int collection, int rune, int layer) { return false; }
    }
}
