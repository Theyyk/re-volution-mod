# UI-03-B — Infection Mark vulnerability channel correction

Owner approval: **universal vulnerability** affects all damage; **poison vulnerability** affects POISON only. The three **Метка заражения / Infection Mark** runes in first collection (legacy slots 22–24, display positions 23–25) must not increase click damage.

## Confirmed root cause

- `PrimaryRuneCatalog` defines Infection Mark with `RuneStatType.POISON_VULNERABILITY`.
- `RuneCollectionCatalog`, `RuneCombatService.accumulate`, `RuneCombatMath` and `RuneDamageMath` already provide channel-specific vulnerability support.
- The canonical Balance Core `default-balance.json` incorrectly configured `runes.poison.effect.infection_mark.1.stat_type=ALL_DAMAGE_VULNERABILITY`, causing actual runtime to apply these rune effects to click and all other channels.
- The v2.3 collection overlay provides rank-4 strength but does not override that stat-type key.

## Safe correction

Replace **only this one Balance Core stat-type value** with `POISON_VULNERABILITY`. Keep all potency values, rank 1–4 scaling, +N enhancements, rune ownership, and universal-vulnerability mechanics unchanged. No MongoDB edits or save migrations. Regression test asserts bundled balance effects at all three slots and all ranks and checks poison-vs-click and universal-vulnerability math.

## Minecraft targeted retest

Verify with same matching DEV client/server build:
1. With Infection Mark equipped, CLICK damage does **not** change when only its poison-vulnerability effect is increased; POISON increases according to existing rank/+N.
2. E hover breakdown must mirror the server and reflect the channel isolation. E currently labels the base Rune contribution but does not itemize every individual primary rune; do not misinterpret overall stable click damage as a poison-only stat.
3. With a genuinely universal vulnerability effect, CLICK and POISON both change, per approved contract.
4. Retest Reset, reconnect and no duplicated effects; no rank or owned rune lost.
5. Manual confirm same candidate SHA and matching DEV server/client; do not mark UI-03-B accepted until QA and DoD.

Stable v2.12.2, PROD and Balance Core numeric rules are untouched.
