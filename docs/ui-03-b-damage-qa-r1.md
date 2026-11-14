# UI-03-B — Minecraft QA R1: damage projection investigation

Candidate: `dev-candidate-43596310734-1-43596430079-1`; source `464a44db3b635cbb2339b57895efde05d04e332f`.
Owner QA: 33 PASS, 0 FAIL, 3 BLOCKED (B04/B06/G03), 0 not tested.

## Owner clarification, authoritative for UI-03-B

- Completion rewards do **not** grant a separate resource. The reward of a successful Run reset is the existing permanent statistical benefit (saved progression/rank levels). The former C07 is PASS under this confirmed contract. No new reset-side payout or balance formula.
- M1 modifiers should activate one by one at each reached rank/threshold; each is automatic, not a GUI-selectable effect. Current `PrestigeService.recordProgress` records newly reached ranks; `contributions` includes only reached active-run ranks. Retest this threshold behavior.

## Observed blocker

Owner reports HUD around 1–1.08 while observed mob hit is approximately 1.5 million, so automatic effect impact, effect termination after Reset, and non-stacking cannot be verified with raw-hit comparisons.

### Audited code paths

- `PlayerStatsSyncService.sendTo` computes `resolvedClickDamage` and `resolvedPoisonDamage` from `RuneCombatMath` stable snapshot and does **not** apply the entire hit processing chain.
- `RpgHudRenderer` displayed those two values without identifying them as stable base rune damage.
- `DamageBonusHandler` uses `RuneCombatMath.clickDamage` when a runic combat snapshot is available, or `PlayerStatsService.getTotalDamageBonusValue` as a legacy additive fallback.
- `PrestigeDamageEffectHandler` multiplies managed-mob damage by current `EffectResolver` Mastery/other contributions. `UniqueRuneDamageHandler`, `GodWorshipDamageHandler`, and `AuraDamageHandler` also modify damage later in the `LivingHurtEvent` pipeline. Proc and temporary effects can further change live damage.
- **Root cause of the exact 1.5M hit is not established from code inspection alone.** This change neither adjusts these bonuses nor suppresses damage.

## Safe fix and diagnostics

- HUD now explicitly labels two numbers as **base** click/poison Rune projections; it no longer silently presents them as complete final hit damage.
- Server-authored Mastery snapshot exposes read-only multipliers for CLICK, POISON, and ALL damage from exactly the existing `PrestigeService.contributions` and the registered Access Gate. It is a QA diagnostic rather than a second gameplay calculation.
- Y shows compact `Mastery x К/Я/Все` values. Inspect before and during active run, after thresholds, after reconnect, and after successful Reset. Permanent best-saved progression may keep multipliers greater than x1 after reset; only temporary rank contributions disappear.
- The packet changes require the matching DEV client and server; never pair with an older binary.

## Retest

- B04: compare server Mastery diagnostic factors before/after an individual reached rank. To assert **actual hit** effect, also verify a controlled mob hit with other multipliers held constant; otherwise remain BLOCKED.
- B06: ensure current-run temporary contributions end on successful Reset; permanent contributions remain.
- G03: reconnect does not add extra copies of effect contributions, and switching between no-run/run does not stack. Trace other damage sources if a huge unexplained hit persists.
- Record factual observed screenshots and server logs for any discrepancy. Do not infer double damage from HUD baseline vs live hit alone.

Protected: five branches, permanent ranks/Tier, Premium Preserve, CAS crystals, location progression and Reset-only stage clearing, permanentSlots, legacy fields, Rune/Pets/Aura, and Crystal/Offerings 1000ms limiter.
