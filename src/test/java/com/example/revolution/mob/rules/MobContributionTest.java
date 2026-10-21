package com.example.revolution.mob.rules;

import com.example.revolution.mob.state.MobContributionLedger;
import java.math.BigDecimal;
import java.util.UUID;

public final class MobContributionTest {
    public static void run() {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");

        MobContributionLedger ledger = new MobContributionLedger();
        ledger.add(a, new BigDecimal("40"));
        ledger.add(b, new BigDecimal("20"));
        check(ledger.amount(a).compareTo(new BigDecimal("40")) == 0, "Player A contribution mismatch");
        check(ledger.amount(b).compareTo(new BigDecimal("20")) == 0, "Player B contribution mismatch");
        check(ledger.total().compareTo(new BigDecimal("60")) == 0, "Contribution total mismatch");
        check(a.equals(ledger.leadingContributor()), "Largest contributor should lead");

        BigDecimal currentHp = new BigDecimal("40");
        BigDecimal returned = ledger.remove(a);
        currentHp = MobContributionRules.restoreHealth(currentHp, new BigDecimal("100"), returned);
        check(currentHp.compareTo(new BigDecimal("80")) == 0,
                "Leaving player must return exactly that player's damage to mob HP");
        check(ledger.amount(a).signum() == 0, "Leaving player contribution must be removed");

        ledger.add(a, new BigDecimal("10"));
        check(ledger.amount(a).compareTo(new BigDecimal("10")) == 0,
                "Re-entered player must start contribution from zero");

        ledger.clear();
        ledger.add(a, new BigDecimal("50"));
        ledger.add(b, new BigDecimal("50"));
        check(a.equals(ledger.leadingContributor()), "Exact tie must go to the player who reached the tied amount first");

        ledger.clear();
        ledger.add(a, new BigDecimal("20"));
        ledger.add(b, new BigDecimal("30"));
        ledger.add(a, new BigDecimal("10"));
        check(b.equals(ledger.leadingContributor()),
                "Tie winner must be based on who reached the final tied contribution first, not first hit");

        BigDecimal reward = new BigDecimal("1000");
        BigDecimal share51 = MobContributionRules.proportionalReward(reward, new BigDecimal("51"), new BigDecimal("100"));
        BigDecimal share49 = MobContributionRules.proportionalReward(reward, new BigDecimal("49"), new BigDecimal("100"));
        check(share51.compareTo(new BigDecimal("510")) == 0, "51% reward share mismatch");
        check(share49.compareTo(new BigDecimal("490")) == 0, "49% reward share mismatch");

        BigDecimal effective = MobContributionRules.effectiveDamage(new BigDecimal("10"), 50.0D);
        check(effective.compareTo(new BigDecimal("10")) == 0,
                "Overkill must contribute only remaining mob HP");
        check(MobContributionRules.restoreHealth(new BigDecimal("90"), new BigDecimal("100"), new BigDecimal("40"))
                        .compareTo(new BigDecimal("100")) == 0,
                "HP rollback must never exceed max HP");

        ledger.clear();
        check(ledger.isEmpty() && ledger.total().signum() == 0 && ledger.leadingContributor() == null,
                "Respawn/unload clear must reset all contribution state");

        System.out.println("v1.3.7 managed mob contribution/reward/HP rollback regression checks passed");
    }

    public static void main(String[] args) { run(); }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
