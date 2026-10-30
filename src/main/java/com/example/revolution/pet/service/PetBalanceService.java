package com.example.revolution.pet.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.pet.definition.PetDefinition;
import com.example.revolution.pet.definition.PetEffectDefinition;
import com.example.revolution.pet.definition.PetRank;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Locale;

/** Typed Pet view over Balance Core. */
public final class PetBalanceService {
    private final BalanceService balance;

    public PetBalanceService(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance is required");
        this.balance = balance;
    }

    public BigDecimal purchaseCost(PetDefinition pet) { return nonNegative(value(pet.purchaseCostValueId)); }
    public BigDecimal effectValue(PetEffectDefinition effect) { return value(effect.balanceValueId); }
    public BigDecimal normalSlotCost(long purchaseNumber) {
        int index = boundedIndex(purchaseNumber <= 1 ? 0L : purchaseNumber - 1L);
        return nonNegative(balance.evaluateActiveCurve("pet.normal_slot.cost", index)).setScale(0, RoundingMode.HALF_UP);
    }
    public BigDecimal premiumSlotCost(int number) { return nonNegative(value("pet.premium_slot." + number + ".crystals")); }
    public long cooldownMillis() { return nonNegative(value("pet.cooldown.hours")).multiply(BigDecimal.valueOf(3_600_000L)).longValue(); }
    public BigDecimal baseXpPerSecond() { return nonNegative(value("pet.xp.per_second")); }
    public BigDecimal xpForNextLevel(BigInteger level) {
        BigInteger safe = level == null || level.signum() <= 0 ? BigInteger.ONE : level;
        BigInteger zeroBased = safe.subtract(BigInteger.ONE);
        int index = zeroBased.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0
                ? Integer.MAX_VALUE : zeroBased.intValue();
        return nonNegative(balance.evaluateActiveCurve("pet.xp.next_level", index));
    }
    public BigDecimal rankScale(PetRank rank) {
        PetRank safe = rank == null ? PetRank.ORDINARY : rank;
        return nonNegative(value("pet.rank." + safe.name().toLowerCase(Locale.ROOT) + ".scale"));
    }
    private BigDecimal value(String key) { return new BigDecimal(balance.getActiveValue(key)); }
    private static BigDecimal nonNegative(BigDecimal value) { return value.signum() < 0 ? BigDecimal.ZERO : value; }
    private static int boundedIndex(long value) {
        if (value <= 0L) return 0;
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
