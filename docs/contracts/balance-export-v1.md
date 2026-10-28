# Balance Export Contract v1

## Назначение

`Balance Export Contract v1` предоставляет внешним инструментам стабильное read-only представление активного Balance Core snapshot.

Контракт предназначен для Control, последующего экспорта в Google Sheets и других внешних интеграций. Сам gameplay-мод не подключается к Google API, OAuth и не хранит внешние credentials.

## Endpoint

`GET /api/v1/balance/export`

Требования:

- используется существующая Admin API loopback/HMAC/auth boundary;
- требуется capability `balance.read`;
- endpoint не изменяет активный balance snapshot;
- request body должен быть пустым.

## Ответ

```json
{
  "exportContractVersion": 1,
  "revision": "<sha256>",
  "balanceSchemaVersion": 1,
  "canonicalConfig": "<canonical Balance Core JSON>",
  "columns": ["pointer", "type", "value"],
  "rows": [
    {
      "pointer": "/balanceSchemaVersion",
      "type": "number",
      "value": "1"
    }
  ]
}
```

### Поля

- `exportContractVersion` - версия этого внешнего export contract. Для текущего формата всегда `1`.
- `revision` - deterministic revision активного Balance Core snapshot.
- `balanceSchemaVersion` - версия внутренней Balance Core schema.
- `canonicalConfig` - canonical JSON активного snapshot. Это authoritative payload.
- `columns` - фиксированный порядок колонок spreadsheet projection.
- `rows` - deterministic leaf projection canonical JSON.

## Rows

Каждая строка содержит:

- `pointer` - RFC 6901 JSON Pointer до leaf-значения в `canonicalConfig`;
- `type` - `string`, `number`, `boolean` или `null`;
- `value` - текстовое представление leaf-значения, `null` для JSON null.

Для массивов используется числовой индекс JSON Pointer, например `/curves/example/values/0`.

Символы в object keys экранируются по RFC 6901:

- `~` -> `~0`;
- `/` -> `~1`.

Точки в balance IDs не имеют специального значения и сохраняются как часть одного key.

## Детерминизм

Для семантически одинакового Balance Core config:

- `revision` одинаковый;
- `canonicalConfig` одинаковый;
- порядок и содержимое `rows` одинаковы.

Decimal values используют canonical формат Balance Core и не переводятся через `double`, `float`, `int` или `long`.

## Границы контракта

`rows` предназначены для табличного чтения и внешнего преобразования. Источником истины остаётся `canonicalConfig` вместе с `revision`.

Контракт не:

- активирует или изменяет balance;
- заменяет `/api/v1/balance/preview` и `/api/v1/balance/apply`;
- подключается к Google Sheets напрямую;
- хранит Google API keys, OAuth tokens или другие внешние credentials;
- создаёт отдельное balance state вне активного `BalanceSnapshot`.
