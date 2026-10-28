# Balance Core

`v1.4.1` introduced the canonical balance foundation. Starting with `v1.4.2`, gameplay domains migrate onto that foundation incrementally instead of keeping economy formulas inside Java services.

The boundary is intentionally small: balance data is parsed into immutable definitions, validated as a complete snapshot, assigned a deterministic revision, and only then may become active. Domain adapters translate generic balance ids into gameplay concepts; the core itself does not depend on rune, mob, stage, or Admin packages.

## Canonical JSON v1

```json
{
  "balanceSchemaVersion": 1,
  "values": {
    "runes.poison.purchase.resource": "coins"
  },
  "curves": {
    "example.fixed": {
      "mode": "FIXED",
      "value": "100"
    },
    "example.table": {
      "mode": "TABLE",
      "values": ["100", "120", "150"]
    },
    "example.formula": {
      "mode": "FORMULA",
      "base": "100",
      "growth": "1.15"
    },
    "example.piecewise": {
      "mode": "PIECEWISE",
      "segments": [
        {
          "from": 0,
          "to": 2,
          "curve": {
            "mode": "TABLE",
            "values": ["100", "120", "150"]
          }
        },
        {
          "from": 3,
          "curve": {
            "mode": "FORMULA",
            "base": "200",
            "growth": "1.20"
          }
        }
      ]
    }
  }
}
```

`values` contains canonical non-numeric settings such as resource ids. Value ids use the same lowercase dotted naming contract as curve ids and participate in the deterministic revision. Empty or blank values are rejected.

Balance numeric values are represented as decimal JSON strings in canonical output and evaluated with `BigDecimal`. Input also accepts JSON numeric primitives. `NaN` and `Infinity` are not valid balance numbers.

## Curve semantics

- `FIXED`: returns the same non-negative value for every non-negative index.
- `TABLE`: contains an explicit contiguous sequence starting at index `0`. Reading beyond the table is an error; the engine never silently clamps or extrapolates.
- `FORMULA`: evaluates `base * growth^index` with `BigDecimal` values.
- `PIECEWISE`: consists of contiguous inclusive ranges starting at global index `0`. Only the final segment may omit `to`; omitted `to` means unbounded. A nested curve receives the local segment index `globalIndex - from`.

These semantics are deliberately deterministic so a future Google Sheets exporter can produce exactly the same runtime inputs without embedding spreadsheet behavior in the game.

## Validation

A snapshot is rejected before activation when, at minimum:

- `balanceSchemaVersion` is unsupported;
- a value or curve id is invalid;
- a canonical string value is blank;
- no curves exist;
- a required curve value is absent or negative;
- a `TABLE` is empty or non-contiguous;
- a `PIECEWISE` definition has gaps, overlaps, invalid bounds, a non-final unbounded segment, or excessive nesting.

Parsing errors and semantic validation errors are separate. Semantic validation aggregates independent errors so Admin/DEV tooling can show more than the first problem.

## Revision and activation

`BalanceConfigCodec` serializes the parsed semantic model into deterministic canonical JSON and calculates a lowercase SHA-256 revision. Property order and insignificant decimal formatting such as `100`, `100.0`, and `100.00` do not change the revision. Canonical string values are sorted by id and are part of the same revision.

`BalanceService.prepare()` parses and validates without changing runtime state. `activate()` publishes one complete immutable `BalanceSnapshot` only after preparation succeeds. A failed activation leaves the previous snapshot unchanged.

## Rune Purchase contract in v1.4.2

Rune purchase is the first gameplay consumer migrated to Balance Core. `RunePurchaseBalanceService` is the rune-domain adapter; generic Balance Core packages remain unaware of statuses or rune ranks.

The bundled baseline uses these ids:

- `runes.<status>.purchase.resource`
- `runes.<status>.duplicate.resource`
- `runes.<status>.purchase.rank.<rank>`
- `runes.<status>.duplicate.rank.<rank>`

The curve index is the number of currently opened unique rune slots in the collection. A successful new rune increments that progression index. A duplicate is still paid for and grants its configured duplicate reward, but does not increment the progression index; therefore the next attempt remains on the same curve point.

The current `ResourceService` stores integer amounts. The rune adapter therefore rejects fractional or out-of-range curve results instead of silently rounding them. Balance Core itself continues to retain `BigDecimal` precision.

`src/main/resources/balance/default-balance.json` is the canonical bundled baseline activated during server composition. In `v1.4.2`, Poison purchase keeps the existing placeholder purchase price of `100` coins while duplicate compensation is `1` rune dust per duplicate. Later balance passes may change those numbers through canonical data without changing purchase logic.

## Rune Enhancement contract in v1.4.3

Enhancement is the second rune economy path migrated to Balance Core. The generic core still has no rune knowledge: `RuneEnhancementBalanceService` translates canonical ids, while `RuneEnhancementPlanningService` owns the pure collection-level planning rule.

The bundled baseline adds:

- `runes.<status>.enhancement.resource`
- `runes.<status>.enhancement.targeting`
- `runes.<status>.enhancement.cost`

`enhancement.targeting` accepts `ALL_SLOTS` or `OWNED_ONLY`. `ALL_SLOTS` preserves the historical Poison behavior in which an unopened fixed slot may receive persistent enhancement levels before its rune is obtained. `OWNED_ONLY` restricts random targeting to currently owned rune slots.

Enhancement price no longer depends on whichever slot RNG selects. Before any random target is chosen, the server calculates:

`TotalEnhancementLevel = Σ slot.enhancementLevel`

Every requested bulk step is then priced sequentially at global indices `TotalEnhancementLevel`, `TotalEnhancementLevel + 1`, and so on. RNG only distributes those already-priced steps over eligible slots. The same global bulk price is copied into every row of the existing enhancement packet, preserving the wire contract while removing per-slot price semantics.

The current integer `ResourceService` boundary remains explicit: fractional costs, costs above the integer range, and unsupported targeting values are rejected instead of rounded or guessed.

The Poison baseline remains intentionally unchanged numerically in `v1.4.3`: enhancement uses `rune_dust`, targeting is `ALL_SLOTS`, and each global enhancement step costs `10`. Future balance passes may replace the fixed curve without changing gameplay code.

## Remaining migrations

Rank-up, rune effects, mob balance, Admin Preview/Apply and export tooling remain on their existing paths until their dedicated `1.4.x` migrations.
