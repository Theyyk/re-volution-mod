# UI-03-B — M1 / R2 owner decision and safe integration

Decision (2026-11-14): automatic Run modifiers (M1), existing fragments plus additional rewards (R2); numerical balance only from already existing Balance Core. No invented formulas or resource grants.

## M1 implementation boundary

The existing automatic Rank contributions are authoritative in `PrestigeService.contributions()` and are the only confirmed Run-specific effects. A snapshot projects their existing rank ID, effect stat, and calculated value through `PrestigeBranchRegistry.RankDefinition.effectAtMetaLevel` to menu Y. The projected value **does not create an additional effect**: the existing `EffectResolver` remains the single gameplay application path. Reconnect does not accumulate stateful modifiers, and the UI does not request or self-select effects. The legacy `runModifiers` Mongo field remains untouched; it is not evidence of an approved modifier catalog. This is a conservative partial M1 integration until Balance Core defines distinct Run modifier entries.

## R2 existing reward evidence

- `prestige.fragment_reward.<branch>.per_level` is bundled in `balance/v2.4.0-pets.json`; all five existing branch values are 1.
- `pet.level_up.fragment_reward` is already configured and Pet pending fragments have their own state.
- Server's authoritative Mastery snapshot already reports current run's spendable fragments, `masteryPendingFragments`, and `petPendingFragments`. These sources are now labeled separately in Y.
- `v2.6.0-run-exchange.json` and `RunExchangeService` define separate player-initiated exchanges for rewards, not an automatic Run-completion grant.
- No approved existing `mastery.run_completion.reward.*` Balance IDs or reset grant rules were found among the bundled Balance resources. Therefore **no additional completion payout is implemented**. R2 completion payout remains BLOCKED until Balance Core has an owner-approved definition. No multiplier is inferred from Premium Preserve.
- No new resource, amount, bonus, Mongo field, or reward packet action is introduced.

## Regression coverage

The new packet fields are sent by the server and decoded by the matching DEV client; verify same-version client/server publishing. Automated packet roundtrip verifies stat and value. Existing Mastery/Reset/persistence tests and PR #391 reward replay/reconnect tests remain required. Manual Minecraft tests must not report additional completion rewards PASS while absent.

## DoD gates still open

No assertion of full M1 modifier catalog, R2 completion grant, DEV client publication, or Minecraft QA is justified by this partial integration. Stable v2.12.2 and PROD remain unchanged.
