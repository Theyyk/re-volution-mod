# RE:volution Mod - Forge 1.12.2

Серверно-авторитетный gameplay-мод RE:volution для Minecraft Forge 1.12.2. Репозиторий содержит основную игровую логику: интерфейс игрока, руны и статусы, экономику ресурсов, managed-мобов, Realm runtime, Location/stage progression, переходы между стадиями и MongoDB persistence.

**Стабильная версия: `v2.9.0`.**

[Скачать актуальный релиз](https://github.com/Theyyk/re-volution-mod/releases/latest) · [Документация](docs/README.md) · [Архитектура](docs/architecture.md) · [Контракт v2.9.0](docs/releases/v2.9.0.md) · [История релизов](docs/releases/README.md)

README описывает только **актуальное состояние проекта**. История изменений хранится в `docs/releases/` и GitHub Releases и здесь не дублируется.

## Границы проекта

Этот репозиторий отвечает за gameplay-мод, работающий на dedicated-сервере и, при полном клиентском режиме, на клиенте Minecraft.

Связанные части экосистемы находятся отдельно:

- **RE:volution Client** - launcher/updater, Auth API, client-only Forge mod и production manifest;
- **RE:volution Resource Pack** - визуальные ассеты и distribution-файлы;
- **RE:volution Control / Admin** - локальные инструменты управления DEV/PROD и игровыми данными.

Gameplay-мод не должен содержать приватные signing keys, секреты Admin API или клиентскую логику обновления.

## Актуальная архитектура

### Realm runtime

- Realm 0 - административный overworld / dimension `0`.
- Realm 1 - dimension `201`, основной player Realm и эталон стартовой синхронизации.
- Realm 2 - dimension `202`.
- Realm 3 - dimension `203`.
- При обычном входе игрок назначается в наименее населённый Realm из `1..3`; Realm 0 в автоматическом распределении не участвует.
- При равной минимальной нагрузке выбор выполняется случайно среди Realm с одинаковым минимумом.
- `/realm join <realmId> [player]` остаётся административной командой и поддерживает Realm 0.
- Перед загрузкой миров `DIM202` и `DIM203` подготавливаются из сохранённого `DIM201`; во время runtime Realm 1/2/3 независимы.
- Runtime managed-мобов не должен принудительно загружать пустые Realm-измерения.

Подробности: [docs/realm-startup-sync.md](docs/realm-startup-sync.md).

### Locations, stages и managed-мобы

- Одна gameplay Location содержит независимые stages, адресуемые парой `locationId + mobSlotId`.
- Каноническое техническое имя stage-моба: `Mob<locationId>_<mobSlotId>`.
- Один spawn-slot соответствует одной физической сущности; несколько spawn-slot могут использовать один логический `mobId`.
- Поддерживаются `FIXED_POINT` и `RANDOM_AREA`, а также `STATIONARY` и `ROAMING`.
- Population отвечает за физическое наполнение stage, а существование progression stage определяется `LocationDefinition`.
- При изменении population через Control/Admin enabled slots нормализуются примерно 50/50: `fixed=floor(n/2)`, `random=ceil(n/2)`. При нечётном значении лишний слот всегда `RANDOM_AREA`.
- Новые fixed-позиции при population update распределяются по геометрии stage, а не клонируют одну центральную координату.
- Финальная configured stage каждой Location автоматически является runtime/persisted boss и единственной `BOSS_ZONE`; при добавлении новой финальной stage boss-status переносится автоматически.
- Existing geometry, transition points и world builds не должны пересоздаваться при обычном reconciliation.
- `RANDOM_AREA` выбирает новую независимую позицию для каждой физической жизни моба.

Ключевые команды и полный admin/gameplay command reference: [docs/commands.md](docs/commands.md).

### Contribution, rewards и progression

- Contribution учитывает фактически снятый HP с ограничением overkill оставшимся HP моба.
- Вклад принадлежит конкретной физической жизни моба и очищается при respawn/unload-load.
- Награда после смерти делится между актуальными участниками пропорционально effective damage.
- Progression credit получает текущий `max contributor`.
- Stage-доступ персональный для игрока и сохраняется в MongoDB.
- Победа на текущей stage открывает следующую; Boss Zone открывает первый configured stage следующей Location.
- Revoke/reset, teleport и GUI/network requests остаются server-authoritative.
- Fast-auto и trigger-zone automation существуют независимо друг от друга.

Подробности текущего поведения: [docs/gameplay.md](docs/gameplay.md).

### Balance Layer в v1.5.0

- `Balance Core` хранит canonical balance snapshot, валидирует его целиком и публикует только после успешной подготовки.
- Canonical config поддерживает числовые curves и string values, а revision/hash детерминирован и не зависит от форматирования JSON.
- `v1.5.0` содержит текущий ZBT Draft snapshot для Location 1 и Collection 1; статус stable относится к реализации и воспроизводимости snapshot, а не означает Final для чисел баланса.
- Purchase progression полностью выводится из фактических per-rune ranks. Вложенный цикл после первого rank-up продолжает собирать нижний ранг вокруг уже повышенных рун, вместо переключения покупок на factual maximum rank.
- Для purchase point используются `lowerProgress` и `higherRuneCount`; исторические `purchaseIndex`, `currentCollectionRank` и `nextPurchaseCost` не являются источником истины.
- Purchase cost рассчитывается из ZBT base curve с pressure `1.15 ^ higherRuneCount`; duplicate reward вычисляется из актуальной цены попытки, выдаёт `rune_dust`, не возвращает coins и не двигает progression.
- Enhancement cost использует текущую ZBT-таблицу `+1..+50` без изменений, а после последней явно заданной точки продолжается data-driven TABLE tail с ростом `1.10`; hard cap усилений отсутствует. Усиление остаётся свойством слота и переживает rank-up/revert.
- Rank-up использует ZBT Shard curve и factual higher-rank count.
- Rune Rank Revert сохраняет `CTRL + ПКМ = Ослабить`, не возвращает ресурсы и читает server-side policy из Balance Core: `revert_allowed_from_rank=3`, `revert_steps=1`.
- Poison трактуется как DPS-бюджет с 5 ticks/sec и AoE total multiplier curve для 1..10 целей; damage делится между целями по текущему числу targets.
- Канонические `Mob1_1 .. Mob1_41` получают ZBT HP/reward overrides из active Balance Core. Stage 41 остаётся fixed boss с `30,000,000 HP` и `15,000,000 coins`, без repeat-kill scaling.
- Admin/Control может получить текущий canonical snapshot и revision, выполнить полную валидацию candidate через Preview и атомарно применить именно подготовленный snapshot через Apply.
- Balance Preview session-bound, revision-bound, одноразовый, имеет TTL 120 секунд и ограниченное in-memory pending state.
- Apply использует atomic expected-revision compare-and-set; stale candidate не может перезаписать более новый active snapshot.
- Admin/Control может read-only экспортировать active Balance snapshot через `GET /api/v1/balance/export`: ответ содержит contract version, revision, schema version, authoritative canonical JSON и deterministic spreadsheet-friendly rows.
- Export rows используют RFC 6901 JSON Pointer и явные типы значений; canonical config и revision остаются источником истины.
- Gameplay-мод не подключается к Google API/OAuth и не хранит внешние integration credentials; Google Sheets остаётся задачей внешнего Control/инструмента.

Подробности: [docs/balance-core.md](docs/balance-core.md) и [docs/releases/v1.5.0.md](docs/releases/v1.5.0.md).

### Admin / Control APIs

- Список игроков включает сохранённые offline-профили.
- Offline-профилям можно устанавливать, добавлять и обнулять ресурсы напрямую по UUID через Mongo.
- Live-only действия - руны, усиления и progression - недоступны офлайн до входа игрока.
- `lastKnownName` сохраняет последнее известное имя профиля.
- Stage editor поддерживает HP, population и reward resource/amount.
- Stage batch workflow: `Validate → Preview → Apply` с session/revision guards и recovery journal.
- Balance workflow: `Read → Preview → Apply` с отдельными `balance.read` / `balance.write`, session/revision guards и атомарной активацией prepared snapshot.
- Balance export: `GET /api/v1/balance/export` под `balance.read`, только read-only active snapshot; write-path не расширяется.
- Balance routes остаются внутри существующей loopback-only HMAC Admin API границы и выполняются через Minecraft server-thread bridge.
- Последняя configured stage остаётся автоматическим boss; обычного ручного dropdown `BOSS_ZONE` нет.

Полный первоначальный контракт Admin/Control этого цикла: [docs/releases/v1.4.0.md](docs/releases/v1.4.0.md).

## Игровые системы

- Адаптивный GUI со статистикой, ресурсами, персонажем, рунами, питомцами, Aura System и навигацией.
- Rune System содержит семь фиксированных коллекций по 26 persistent slots каждая; Collection 1 сохраняет существующий CLICK + POISON контракт.
- Следующая коллекция permanently открывается после первого достижения `13/13` factual рун ранга `IMPROVED` или выше в предыдущей коллекции и после этого не закрывается.
- Pet System содержит 8 run-scoped normal slots и 2 permanent premium slots, server-authoritative покупку/выбор, Awakening, cooldown и Rank Revert.
- Обычные и улучшенные Pets сбрасываются при Prestige, а Advanced+ сохраняют ownership, rank, level, XP и выбранное состояние; premium entitlement сохраняется отдельно.
- Все активные Pets получают online-only time-based XP без hard cap уровня; Pet XP bonus участвует в том же shared Effect Resolver.
- Aura System хранит постоянную коллекцию Aura без уровней и XP. Одна Aura может быть активна одновременно; active/pinned Aura защищены от craft/dismantle, а Mastery Reset не затрагивает коллекцию.
- Aura Chest расходует `aura_key`, поддерживает открытия `x1/x3/x5/x10` и при нехватке ключей может после явного подтверждения купить ровно недостающее количество за Crystals по Balance Core.
- Aura instances сохраняют rarity, origin, generation seed, raw rolls и aggregated effects. Дубликаты эффектов внутри одной Aura разрешены и аддитивно агрегируются.
- Craft создаёт новую Aura следующей rarity из настраиваемого числа Aura текущей rarity; dismantle создаёт настраиваемое число новых Aura предыдущей rarity. Результаты всегда генерируются заново.
- `Фрагменты знаний` используют существующий `runBranchCurrency`: покупают normal Pet slots и участвуют в server-authoritative Run Exchange с 7 baseline offers - Pet Slot за Fragments и Coins/Rune Dust/Shards за Fragments или Crystals; resource rewards считаются как процент от фактического текущего баланса, а числовые ZBT-параметры остаются data-driven.
- Damage/status foundation поддерживает `CLICK`, `POISON`, `BURNING`, `BLEEDING`, `CURSE`, `WITHER` и `DECAY` через общий server-authoritative damage boundary.
- Ранги рун: **Обычная → Улучшенная → Продвинутая → Мифическая**; `ANCIENT` зарезервирована и не участвует в текущем gameplay.
- Покупка рун `x1`, `x5`, `x10`, `x100` с серверной проверкой экономики и блокировкой кнопки при нехватке полной стоимости выбранного количества.
- Дубликаты, Пыль рун, пробуждение и постоянные уровни усиления `+N` обрабатываются сервером.
- Для подходящей руны `CTRL + ПКМ` выполняет одноступенчатый Rank Revert; левый клик сохраняет существующее действие пробуждения.
- Усиление и ранг - независимые множители; базовая формула: `finalPower = basePower × (1 + enhancementLevel × enhancementStep)`.
- Для direct melee по managed-мобам используется cadence в стиле pre-1.9: ванильный attack cooldown не режет урон, а target hurt-resistance не отбрасывает быстрые клики; projectile/secondary damage этим bypass не затрагиваются.
- Poison tick использует отдельный managed-mob combat pause и временно останавливает движение цели.
- Vanilla survival HUD, голод, player damage и разрушение блоков отключены для RPG Survival runtime; Creative-администраторы сохраняют строительные возможности.
- Кастомный RPG HUD показывает ресурсы снизу по центру и финальные click/poison значения слева сверху.
- Поддерживаются упрощённый, полный и инженерный форматы больших чисел.
- Основные ресурсы: `coins`, `crystals`, `rune_dust`, `rune_shards`, `rune_cores`, `aura_key`.
- Серверный контракт ресурсов не допускает отрицательных балансов.
- Данные игроков и progression сохраняются в MongoDB.

Основной gameplay GUI открывается клавишей `E`, меню быстрого перехода - клавишей `L`, настройки формата больших чисел - клавишей `P`. Функциональное Aura menu открывается клавишей `U`.

## DEV / PROD

- `develop` - основная ветка разработки и DEV acceptance.
- `main` - стабильная ветка.
- DEV и PROD используют отдельные Minecraft runtime/world и Mongo namespace.
- DEV build сам по себе не становится PROD.
- Stable promotion проходит отдельно через проверенное дерево, `main`, release tag и release workflow.
- При контролируемой PROD synchronization world и Mongo заменяются DEV snapshot только после backup; это не merge данных.
- Runtime rollback опирается на сохранённые snapshots, а не на Git.

Основной интерфейс инфраструктуры - `REvolutionControl.exe`.

## Установка вручную

Ручная установка - fallback для разработки и диагностики. Для обычного игрока используется RE:volution Client.

1. Установите Java 8 и Minecraft Forge **1.12.2 / 14.23.5.2859**.
2. Скачайте `RE-volution-<version>.jar` из [актуального GitHub Release](https://github.com/Theyyk/re-volution-mod/releases/latest).
3. Поместите JAR в `mods` dedicated-сервера и клиента, если используется полный modded-client режим.
4. Настройте MongoDB и серверную конфигурацию.
5. Для GUI установите официальный RE:volution Resource Pack.

Полная настройка: [docs/setup.md](docs/setup.md).

## Resource Pack

Актуальные ассеты публикуются отдельно в [RE:volution Resource Pack](https://github.com/Theyyk/re-volution-resource-pack/releases/latest). Resource Pack отвечает за визуальные ресурсы; состояние, геометрия GUI и игровая логика находятся в моде.

Namespace rune-ассетов: `customguimod`. При отсутствии нужной текстуры gameplay GUI использует безопасный vanilla fallback.

## Разработка и проверки

Gameplay-изменения проходят через feature/fix branch → PR в `develop` → exact-head CI → обычный merge → post-merge CI. Stable promotion в `main` выполняется отдельно; squash/rebase для release history не используются.

Перед stable promotion CI дополнительно требует, чтобы version metadata, строка стабильной версии README и ссылка README на текущий release contract совпадали. Поэтому тег нельзя штатно выпустить с устаревшим README.

CI проверяет:

- согласованность версии, README и release descriptions;
- JUnit и standalone regression tests;
- architecture/security guards;
- Forge build и содержимое итогового JAR;
- Realm/startup contracts;
- DEV/stable publication invariants.

Runtime smoke нужен только для поведения, которое нельзя доказать автоматическими тестами.

Подробный процесс: [docs/development.md](docs/development.md). CI scheduling, cache, concurrency и artifact policy: [README-CI.md](README-CI.md).

## Документация и релизы

- [Оглавление документации](docs/README.md)
- [История русских release contracts](docs/releases/README.md)
- [GitHub Releases](https://github.com/Theyyk/re-volution-mod/releases)
- [Архитектура](docs/architecture.md)
- [Gameplay](docs/gameplay.md)
- [Команды](docs/commands.md)

Версионные документы фиксируют состояние конкретного релиза и не переписываются под будущие функции.
