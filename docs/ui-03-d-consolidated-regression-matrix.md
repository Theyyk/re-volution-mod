# UI-03-D — Consolidated Functional Regression Matrix

Baseline: `develop@7dd403bea086ff1759d762161c0ed46bb0f77c08` (14 Nov 2026).
Stable reference: `v2.12.2`; do not modify `main` or tag for this task.
Scope: existing systems only; no balance or gameplay rule changes.

## Evidence levels

- **SOURCE**: relevant code/test exists and was inspected; not proof of a passing run.
- **CI**: exact-head `clean build` + JavaExec regressions + JUnit results, to be recorded at PR head.
- **MC**: owner-run in-game acceptance on the published DEV candidate.
- **N/A**: not currently testable with ordinary gameplay content; not PASS.

| Domain | Required invariants | Existing automated coverage (SOURCE) | UI-03-D follow-up |
| --- | --- | --- | --- |
| A Mastery | five branches, permanent ranks/Tier, M1 automatic effects, R2 fragments and permanent bonus without Run currency, legal accumulated multipliers | `MasteryResetTest`, `MasteryRunRewardLifecycleRegressionTest`, `PrestigeBranchLifecycleTest`, `PrestigeEffectAccessGateTest` | MC A01–A07, H01/H03 |
| B Reset & Premium | ordinary/premium ordinary/mastery/premium mastery, server preview, single Crystal charge, duplicate run-id rejection | `PrestigePremiumOrdinaryResetTest`, `MasteryResetTest`, `MongoPrestigePremiumWalletTest` | MC A05–A07, F04, H02 |
| C Location | progresses without Run, Start Run preserves stages, Reset-only cycle clearing, persistent admin grants, start return/pending return | `MobProgressionServiceJUnitTest`, `MongoMobProgressionLegacyVisibilityTest`, `StageTransitionServiceJUnitTest` | MC B01–B05, H04; real teleport/reconnect |
| D Rune | selective wipe ORDINARY/IMPROVED; ADVANCED+, upgrades, Unique retained; temporary buffs; POISON-only infection vulnerability | `RunePrestigeResetServiceTest`, `PoisonInfectionMarkTargetRegressionTest`, `RuneTransientEffectRuntimeTest`, `UniqueRuneServiceTest` | MC C01–C06 |
| E Pets | 8 ordinary + 2 premium slots, level/awakening, preserve, no double stacking, isolation | `PetSystemTest`, `PetPacketTest`, `MongoPetRepositoryTest` | MC D01–D03, D06 |
| F Aura | unlock/HUD U, equip, crafting, costs, persist, snapshots, blur | `AuraSystemTest`, `MongoAuraRepositoryTest`, `AuraSlotStateTest`, `AuraCraftSelectionTest` | MC D04–D06, G01/G02 |
| G Combat | click/poison/burning separated; permanent + tick-aware temporary modifiers; E projection updates and hover | `PrestigeDamageSourceIntegrationTest`, `GameplayDamageContextTest`, `RuneLiveCombatProjectionTest`, `PlayerStatsDamageProjectionPacketTest` | MC E01–E07 |
| H Resources & Rewards | fragments isolated, authoritative debit, rejected operation inert, no duplicate payout, player isolation | `MasteryRunRewardLifecycleRegressionTest`, `PrestigeResourceBoundaryTest`, `ResourceServiceTest` | MC F01–F05 |
| I Network & Performance | `CombatProjectionPacket` discriminator 31; ordinary 200 ms; shared Crystal/Offerings 1000 ms prequeue; no per-frame E polling | `MasteryNetworkPacketTest`, `NetworkWireContractTest`, `RunePurchaseRequestLimiterTest` | Concurrent mixed-purchase test added in UI-03-D; MC G04–G06 |
| J Persistence | legacy/current BSON, active Run, permanent progress, location return, rune/pet/aura, restart | `MongoPrestigeMigrationTest`, `MongoSchemaContractTest`, `MongoAuraRepositoryTest`, `MongoPetRepositoryTest` | MC H01–H05 and DEV restart when safe |

## Explicit contract and deferrals

- M1 auto-applies Mastery effects; R2 does **not** create separate Run completion currency.
- No cap or deletion of legitimately accumulated permanent Mastery multipliers.
- Vulnerability: `POISON_VULNERABILITY` affects poison only; `ALL_DAMAGE_VULNERABILITY` covers supported channels.
- E includes temporary Rune buffs and updates after expiry; `CombatProjectionPacket` uses ID **31**.
- Location unlocks without active Mastery Run; **starting** a Run does not clear stages; successful **Reset** clears cycle and returns player to configured start.
- Premium Preserve honors existing policy and deducts Crystal exactly once.
- Crystal/Offerings share a **1000 ms per-player** prequeue limiter; ordinary purchase interval is **200 ms**.
- V03 Universal Vulnerability: **N/A — deferred** until a normal gameplay source exists.
- Full multiplayer manual QA: **DEFERRED — PRE-PROD** by owner; automated player-isolation tests remain mandatory. Confirmed multiplayer bugs must still be fixed or documented as blockers.

## Acceptance ledger

| Gate | Status | Evidence |
| --- | --- | --- |
| Baseline GitHub refs | VERIFIED | develop `7dd403b...`, main `19f2ce3...`; zero open PRs at audit start |
| Source-level test inventory | PARTIAL SOURCE AUDIT | Existing test paths enumerated; focused tests inspected |
| Added concurrent Crystal/Offerings test | COMMITTED TO FEATURE | Run with Gradle `runePurchaseRateLimitRegression` / `clean build` |
| Exact-head CI | PENDING | Record jobs, workflow URLs and exact SHA |
| Normal merge | PENDING | Only after exact-head CI and review |
| Post-merge CI | PENDING | Check develop merge SHA |
| Immutable DEV candidate | PENDING | Record Gameplay SHA, JAR SHA-256, Gameplay/Server OCI digests and client publication |
| Minecraft Manual QA | NOT TESTED | 47 checks, owner-operated, comments/logs and full report |
| V03 | N/A — DEFERRED | No artificial source or rune |
| Full Multiplayer QA | DEFERRED — PRE-PROD | Not counted in UI-03-D DoD |

Do not turn SOURCE into TESTED, historical UI-03-C PASS into UI-03-D PASS, or missing production publication into a claimed candidate.
