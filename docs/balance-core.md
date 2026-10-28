# Balance Core

`v1.4.1` introduces the canonical balance foundation without switching existing gameplay consumers yet.

The boundary is intentionally small: balance data is parsed into immutable definitions, validated as a complete snapshot, assigned a deterministic revision, and only then may become active. Existing rune and mob services continue using their current configuration until their dedicated migration releases.

## Canonical JSON v1

```json
{
  "balanceSchemaVersion": 1,
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
- no curves exist;
- a curve id is invalid;
- a required value is absent or negative;
- a `TABLE` is empty or non-contiguous;
- a `PIECEWISE` definition has gaps, overlaps, invalid bounds, a non-final unbounded segment, or excessive nesting.

Parsing errors and semantic validation errors are separate. Semantic validation aggregates independent errors so Admin/DEV tooling can show more than the first problem.

## Revision and activation

`BalanceConfigCodec` serializes the parsed semantic model into deterministic canonical JSON and calculates a lowercase SHA-256 revision. Property order and insignificant decimal formatting such as `100`, `100.0`, and `100.00` do not change the revision.

`BalanceService.prepare()` parses and validates without changing runtime state. `activate()` publishes one complete immutable `BalanceSnapshot` only after preparation succeeds. A failed activation leaves the previous snapshot unchanged.

## Scope of v1.4.1

This release creates the foundation only. It does not yet replace `RuneEconomyConfig`, change purchase/enhancement/rank behavior, migrate mob values, load a balance file during server bootstrap, or expose Admin Preview/Apply. Those consumers are migrated in later `1.4.x` releases after this contract is stable.
