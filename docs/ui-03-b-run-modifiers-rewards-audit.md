# UI-03-B — Run Modifiers & Rewards: verified baseline audit

Baseline: `develop` `0a20f86d472547410b5db2bfc912abe56eacc953`. Scope: read-only review and regression tests, **not a new balance or gameplay contract**.

## Existing contract

- **Mastery foundation:** `PrestigeService` owns active runs, progress, rank effects, pending fragments and reset entrypoints. Five branches in `PrestigeBranchRegistry`: CLICK, POISON, COINS, RUNE_RESOURCES, ALL_DAMAGE.
- **Rank effects:** `PrestigeService.contributions()` calculates `EffectResolver.Contribution` from reached ranks and permanent progress. The calculation is read-only, with source identifiers containing runId; it does not append a stack of effects on reconnect. `PrestigeDamageEffectHandler` consumes resolved damage multipliers. Existing numeric rank effects are labeled technical ZBT placeholders in `PrestigeBranchRegistry`.
- **Run modifiers:** `PrestigeState.RunState.runModifiers` is copied and persisted as `prestige.activeRun.runModifiers` by `MongoPrestigeRepository`, including legacy BSON reads. No modifier catalog, eligibility policy, selection action, or effect application was found in the inspected Prestige service, branch registry, Mastery packets/handlers, GUI, or Balance Mastery tier policy. The field alone does not establish executable modifiers. **Do not expose fictitious selectors or bonuses.**
- **Fragment rewards:** `PrestigeService.recordProgress` adds `rewardPerLevel * delta(branchLevel)` to `masteryPendingFragments` when an active matching branch gains levels. `ServerServices` configures the provider using active Balance value `prestige.fragment_reward.<branch>.per_level`. `collectFragments(player, runId, source)` moves pending Mastery or Pet fragments to the existing spendable run balance and clears the relevant pending field. Wrong run ID and repeated collection return zero. The server snapshot already exposes `fragments`, `masteryPending` and `petPending`; no separate reward preview contract exists.
- **Run Exchange:** `RunExchangeService` and `RunExchangeCatalogFactory` provide an existing separate catalog and Balance-backed price/reward percentage previews for exchanges, not a completion reward inferred from Mastery Reset. No such exchange should be relabeled as a run-completion reward.
- **Reset:** `PrestigeResetService` validates expected runId; `MongoPrestigeRepository.commitReset` uses conditional Mongo update and atomic crystal/pets/runes writes for accepted reset policy. `MasteryActionPacketHandler` provides authoritative previews and snapshots. Premium Preserve is a retention option, not a reward multiplier.
- **Location:** `PrestigeService.unlockRunStage` records current-cycle progress independently of active run, preserving legacy `runUnlockedStages` read compatibility.
- **Network/UI:** Existing Mastery packet includes branch/rank state, pending and collected fragments, and reset previews but no modifier definitions. UI has a manual refresh action rather than render-loop polling.

## Evidence-based matrix

| Area | Status | Action |
|---|---|---|
| Run modifier state/BSON compatibility | EXISTING | Preserve; expand migration coverage if new fields become necessary |
| Modifier definitions, eligibility, selection, application | NOT CONFIRMED | Block implementation pending approved definitions; no invented numeric effects |
| Existing rank effect calculation | EXISTING | Preserve and regression-test reconnect |
| Mastery fragment calculation and collection | EXISTING | Regression tests for duplicate/stale/isolation |
| Mastery reward preview before collection | PARTIAL | Existing server snapshot exposes pending value; no separate predicted-completion reward |
| Mastery Run completion rewards | NOT CONFIRMED | No new grant on Reset or snapshot |
| Mastery GUI reward state | PARTIAL | Existing pending/collected values displayed |
| Mastery GUI selectable modifiers | NOT CONFIRMED | Do not add fictitious controls |
| Reset idempotency and premium wallet | EXISTING | Preserve existing repository CAS and regression tests |

## Owner decisions required before enabling new modifier functionality

**M0 — no new modifiers (safe baseline):** retain current rank effects, document that selectable modifiers are not configured, avoid balance impact. Enables a narrowly scoped integration/reliability deliverable.

**M1 — automatic Run modifiers:** require owner-approved mapping of branch, rank, Tier, effect stat, value, lifecycle and conflicts. Server derives the effect; no client selection.

**M2 — selectable Run modifiers:** requires all M1 definitions plus unlock rules, conflict rules, max selections, price if any, and persistence/selection semantics. Client only sends requests.

For rewards, **R0** retains the existing Balance-backed per-level Mastery/Pet fragment collection with no extra Run completion payout (safe baseline). **R1** adds a Reset/completion reward only after owner supplies trigger, resource, numerical formula, cap, Premium Preserve interaction, eligibility, and idempotent transaction contract.

## Safeguards

Do not change `main`, tag `v2.12.2`, PROD, Rune/Pets/Aura reset semantics, location-cycle reset, `permanentSlots`, or Crystal/Offerings 1000 ms prequeue limiter. Multi-player end-to-end QA remains deferred; test in-process player isolation separately. No claim of Minecraft QA or DEV publication follows from this audit.
