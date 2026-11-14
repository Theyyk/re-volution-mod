package com.example.revolution.resource.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.revolution.resource.repository.ResourceRepository;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** No Mongo instance required: tests the retry/epoch protocol at the ResourceService port. */
public final class ResourceResetConcurrencyTest {
    @Test public void staleEarnedRewardCannotRestorePreResetBalanceOrProgress() {
        MemoryWallet wallet = new MemoryWallet("100");
        wallet.resetBeforeNextCas = true;
        ResourceService service = new ResourceService(wallet);
        final int[] earnedEvents = {0};
        service.setGameplayEventSink(new com.example.revolution.gameplay.GameplayEventSink() {
            @Override public void earnedResource(UUID id, String resource, BigDecimal amount) {
                earnedEvents[0]++;
            }
        });
        assertFalse(service.grantEarnedPlayerResource(UUID.randomUUID(), "coins", BigDecimal.TEN));
        assertEquals(0, wallet.value.compareTo(BigDecimal.ZERO));
        assertEquals("run-2", wallet.epoch);
        assertEquals(0, earnedEvents[0]);
    }

    @Test public void competingWalletUpdateRetriesInSameGeneration() {
        MemoryWallet wallet = new MemoryWallet("100");
        wallet.competitorBeforeNextCas = true;
        ResourceService service = new ResourceService(wallet);
        assertTrue(service.grantPlayerResource(UUID.randomUUID(), "rune_dust", BigDecimal.TEN));
        assertEquals(0, wallet.value.compareTo(new BigDecimal("115")));
    }

    @Test public void staleSpendDoesNotChargeNewRun() {
        MemoryWallet wallet = new MemoryWallet("100");
        wallet.resetBeforeNextCas = true;
        ResourceService service = new ResourceService(wallet);
        assertFalse(service.spendPlayerResource(UUID.randomUUID(), "rune_cores", BigDecimal.ONE));
        assertEquals(0, wallet.value.signum());
    }

    private static final class MemoryWallet implements ResourceRepository {
        BigDecimal value;
        String epoch;
        boolean resetBeforeNextCas;
        boolean competitorBeforeNextCas;
        MemoryWallet(String initial) { value = new BigDecimal(initial); }
        @Override public void purgeDeprecatedLightnings() {}
        @Override public void purgeDeprecatedLightnings(UUID uuid) {}
        @Override public BigDecimal getPlayerResourceValue(UUID id, String canonical) { return value; }
        @Override public void writePlayerResource(UUID id, String canonical, BigDecimal next) { value = next; }
        @Override public ResourceSnapshot resourceSnapshot(UUID id, String canonical) {
            return new ResourceSnapshot(value, value.toPlainString(), epoch);
        }
        @Override public boolean compareAndSetPlayerResource(UUID id, String canonical,
                                                              ResourceSnapshot observed, BigDecimal next) {
            if (resetBeforeNextCas) {
                resetBeforeNextCas = false;
                value = BigDecimal.ZERO;
                epoch = "run-2";
                return false;
            }
            if (competitorBeforeNextCas) {
                competitorBeforeNextCas = false;
                value = value.add(BigDecimal.valueOf(5));
                return false;
            }
            if (!Objects.equals(epoch, observed.resetEpoch) || value.compareTo(observed.value) != 0)
                return false;
            value = next;
            return true;
        }
    }
}
