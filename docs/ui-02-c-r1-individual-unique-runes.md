# UI-02-C-R1 — Per-rune state and non-destructive v1 → v2 migration design

Scope: Issue #314, branch `feature/v2.13.0-ui-02-c-r1-individual-unique-runes`. Target: `develop`.

## Observed v1 state and critical failure
`CollectionState` stores `currentRarity`, `owned` (runeId → factual rarity), `enhancement`, `rollbackRarity`, `rollbackOwned`. `awaken` copies `owned` into collection-wide rollback, clears ownership, promotes only selected rune. `rollback` restores entire collection. `MongoUniqueRuneRepository` writes `players.uniqueRuneState` at `schemaVersion=1`. Crystal/Daily/Offerings acquisition uses collection-wide `currentRarity` and `ownsAtCurrent`; the actual E-menu is `RunesGuiScreen`, not only `UniqueRunesGuiScreen`.

## Proposed v2 authoritative model
- Keep the `owned` map as authoritative runeId → **individual** rarity; never infer it from `currentRarity`.
- Add `runeRollback` map, runeId → previous valid rarity. Only newly performed successful individual awaken operations populate this. Rollback consumes the selected entry once.
- Retain `enhancement` and `lastDailyClaimDay` without resetting them.
- Retain `currentRarity` only as an explicitly named **legacy acquisition tier**, not as authority for individual rune rank or rollback eligibility. A collection is fully acquired when all 26 distinct rune IDs are owned, regardless of their ranks. Existing v1 partial higher-tier saves must continue granting new runes at their existing acquisition tier.
- On duplicate acquisition, consider any owned rune a duplicate, even at another rarity. Never overwrite its rarity or enhancement; preserve Pearl policy and all balance-core rates.
- Add per-entry server-computed `canAwaken` and `canRollback` to snapshot. Client actions only request, never authorize, mutations.
- A valid targeted awaken requires ownership, known rune identity, unlock, complete 26/26 owned set, supported next rank, and fresh nonce. It updates only selected rune rarity and rollback history.
- A valid targeted rollback requires selected owned rune and its **own** recorded previous rank, fresh nonce, unlock and server-side cost check. It consumes this history once. A missing or ambiguous legacy individual previous rank fails closed before spending.
- Gameplay bonus computations continue using the actual owned rune rarity and enhancement.

## Migration rules and examples
- v1 empty → v2 empty; retain daily history.
- v1 partial `owned={A:IMPROVED,B:IMPROVED}` → same factual ownership/rarities/enhancement, **no** invented per-rune rollback.
- v1 `owned={A:ADVANCED}`, `rollbackOwned={A:IMPROVED,B:IMPROVED}` → A remains ADVANCED, B remains absent; preserve the original legacy record for diagnostics, and block A individual rollback until an unambiguous rule has been proven. No collection-wide restore.
- v1 `owned={A:IMPROVED}`, `rollbackOwned={A:COMMON}` → keep A IMPROVED, retain raw historical record, do not infer a usable per-rune rollback merely from this legacy collection snapshot.
- v2 read → write → read must retain the same v2 authoritative maps, daily history and preserved original legacy record, with no rune grants or deletions.
- Unknown future schema and malformed source entries should fail closed rather than silently overwriting the source.
- No server startup/codec pass by itself writes to real player documents. Persistence upgrade occurs only when the game legitimately saves, and real DEV deployment must have a readable, restorable backup first. **No PROD migration.**

## Release gates
Fixtures and tests, exact-head CI, ordinary merge, post-merge CI and DEV publication are separate checks. Legacy DEV data backup/readability and copy-test are hard blockers before any real-data migration. Minecraft Manual QA on the published DEV build remains a separate required gate; NOT TESTED is not PASS. No final handoff until manual PASS.

## QA follow-up R2 — owner-approved DEV rules (2026-11-07)
- DEV save backup requirement waived by owner. No PROD operations.
- **Offerings collection tier cycle:** after acquiring all 26 at a tier, clicking one rune to awaken consumes that tier's owned set and leaves the selected rune alone at the next tier. The next purchases use `currentRarity` (the newly promoted tier). Previous lower-tier runes are not active/owned afterward. This is an explicit owner change from the initial R1 non-destructive invariant, scoped **only to Offerings**, not Crystal or Daily.
- **Offerings targeted rollback:** only the selected newly awakened rune moves one rank down; it does NOT restore the 25 consumed runes, and the acquisition tier stays at its promoted tier. Mixed owned rarities are possible afterward. Confirm in Manual QA.
- **Crystal MYTHIC legacy reset:** MYTHIC remains maximum tier, so awakening cannot exceed MYTHIC. When all 26 Crystal runes are owned and a MYTHIC rune has no individual rollback history, a confirmed Ctrl+right-click is a **new explicit paid reset** to ADVANCED for that rune only. The operation does not claim to reconstruct historical v1 `rollbackOwned`, which remains diagnostic only. The owner can then reawaken that rune to MYTHIC. Other rune rarities and enhancements remain intact. No such fallback is allowed for Daily/Offerings.
- **Daily UTC guard:** same or earlier `epochDay` is blocked; reconnect and clock reversal must not issue a duplicate.
- All behaviors require exact-head automated tests and post-publication Minecraft Manual QA. Existing v1/v2 data under PROD remains untouched.
