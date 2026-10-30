package com.example.revolution.pet.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.pet.definition.PetDefinition;
import com.example.revolution.pet.definition.PetEffectDefinition;
import com.example.revolution.pet.definition.PetRank;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

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
        int index = purchaseNumber <= 1 ? 0 : purchaseNumber - 1 > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) purchaseNumber - 1;
        return nonNegative(balance.evaluateActiveCurve("pet.normal_slot.cost", index)).setScale(0, RoundingMode.HALF_UP);
    }
    public BigDecimal premiumSlotCost(int number) { return nonNegative(value("pet.premium_slot." + number + ".crystals")); }
    public long cooldownMillis() { return nonNegative(value("pet.cooldown.hours")).multiply(BigDecimal.valueOf(3_600_000L)).longValue(); }
    public BigDecimal baseXpPerSecond() { return nonNegative(value("pet.xp.per_second")); }
    public BigDecimal xpForNextLevel(BigInteger level) {
        BigInteger safe = level == null || level.signum() <= 0 ? BigInteger.ONE : level;
        return nonNegative(value("pet.xp.level_base")).multiply(new BigDecimal(safe));
    }
    public BigDecimal rankScale(PetRank rank) { return nonNegative(value("pet.rank." + rank.name() + ".scale")); }
    public BigDecimal autoclickCps() { return nonNegative(value("pet.autoclick.cps")); }
    private BigDecimal value(String key) { return new BigDecimal(balance.getActiveValue(key)); }
    private static BigDecimal nonNegative(BigDecimal value) { return value.signum() < 0 ? BigDecimal.ZERO : value; }
}
