# RE:volution Mod - Forge 1.12.2

Серверно-авторитетный gameplay-мод RE:volution для Minecraft Forge 1.12.2. Репозиторий содержит основную игровую логику: интерфейс игрока, руны и статусы, экономику ресурсов, managed-мобов, Realm runtime, Location/stage progression, переходы между стадиями и MongoDB persistence.

**Стабильная версия: `v1.4.3`.**

[Скачать актуальный релиз](https://github.com/Theyyk/re-volution-mod/releases/latest) · [Документация](docs/README.md) · [Архитектура](docs/architecture.md) · [Контракт v1.4.3](docs/releases/v1.4.3.md) · [История релизов](docs/releases/README.md)

README описывает только **актуальное состояние проекта**. История изменений хранится в `docs/releases/` и GitHub Releases и здесь не дублируется.

## Границы проекта

Этот репозиторий отвечает за gameplay-мод, работающий на dedicated-сервере и, при полном клиентском режиме, на клиенте Minecraft.

Связанные части экосистемы находятся отдельно:

- **RE:volution Client** — launcher/updater, Auth API, client-only Forge mod и production manifest;
- **RE:volution Resource Pack** — визуальные ассеты и distribution-файлы;
- **RE:volution Control / Admin** — локальные инструменты управления DEV/PROD и игровыми данными.

Gameplay-мод не должен содержать приватные signing keys, секреты Admin API или клиентскую логику обновления.

## Актуальная архитектура

### Realm runtime

- Realm 0 — административный overworld / dimension `0`.
- Realm 1 — dimension `201`, основной player Realm и эталон стартовой синхронизации.
- Realm 2 — dimension `202`.
- Realm 3 — dimension `203`.
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

### Balance Layer и rune economy в v1.4.3

- `Balance Core` хранит canonical balance snapshot, валидирует его целиком и публикует только после успешной подготовки.
- Canonical config поддерживает числовые curves и string values, а revision/hash детерминирован и не зависит от форматирования JSON.
- Rune purchase для status-коллекций читает resource ids, стоимость и duplicate reward через Balance Layer.
- Purchase progression индексируется количеством реально открытых уникальных slots; дубликат оплачивается, но progression не увеличивает.
- Rune Enhancement также работает через Balance Layer: resource, targeting и cost curve задаются canonical config.
- Цена усиления индексируется глобальным `TotalEnhancementLevel = Σ slot.enhancementLevel`, а не уровнем случайно выбранного слота.
- Bulk-усиления считаются последовательными глобальными шагами; RNG влияет только на распределение уже оплаченных усилений по допустимым slots.
- Поддерживаются targeting-режимы `ALL_SLOTS` и `OWNED_ONLY`; текущий Poison baseline использует `ALL_SLOTS`.
- Для Poison сохраняются текущие placeholder-значения: покупка `100 coins`, дубликат `1 rune_dust`, усиление `10 rune_dust` за глобальный шаг.
- Rank-up, rune effects и mob balance переводятся на Balance Layer в следующих `1.4.x`.

Подробности: [docs/balance-core.md](docs/balance-core.md) и [docs/releases/v1.4.3.md](docs/releases/v1.4.3.md).

### Admin / Control APIs

- Список игроков включает сохранённые offline-профили.
- Offline-профилям можно устанавливать, добавлять и обнулять ресурсы напрямую по UUID через Mongo.
- Live-only действия — руны, усиления и progression — недоступны офлайн до входа игрока.
- `lastKnownName` сохраняет последнее известное имя профиля.
- Stage editor поддерживает HP, population и reward resource/amount.
- Batch workflow: `Validate → Preview → Apply` с session/revision guards и recovery journal.
- Последняя configured stage остаётся автоматическим boss; обычного ручного dropdown `BOSS_ZONE` нет.

Полный первоначальный контракт Admin/Control этого цикла: [docs/releases/v1.4.0.md](docs/releases/v1.4.0.md).

## Игровые системы

- Адаптивный GUI со статистикой, ресурсами, персонажем, рунами и навигацией.
- Multi-status foundation для рун.
- Статус **Яд** с фиксированными позициями и отдельными rune-иконками.
- Ранги рун: **Обычная → Улучшенная → Продвинутая**.
- Покупка рун `x1`, `x5`, `x10`, `x100` с серверной проверкой экономики.
- Дубликаты, Пыль рун, пробуждение и постоянные уровни усиления `+N` обрабатываются сервером.
- Усиление и ранг — независимые множители; базовая формула: `finalPower = basePower × (1 + enhancementLevel × enhancementStep)`.
- Поддерживаются упрощённый, полный и инженерный форматы больших чисел.
- Основные ресурсы: `coins`, `crystals`, `rune_dust`, `rune_shards`, `rune_cores`.
- Серверный контракт ресурсов не допускает отрицательных балансов.
- Данные игроков и progression сохраняются в MongoDB.

Основной gameplay GUI открывается клавишей `C`, меню быстрого перехода — клавишей `L`.

## DEV / PROD

- `develop` — основная ветка разработки и DEV acceptance.
- `main` — стабильная ветка.
- DEV и PROD используют отдельные Minecraft runtime/world и Mongo namespace.
- DEV build сам по себе не становится PROD.
- Stable promotion проходит отдельно через проверенное дерево, `main`, release tag и release workflow.
- При контролируемой PROD synchronization world и Mongo заменяются DEV snapshot только после backup; это не merge данных.
- Runtime rollback опирается на сохранённые snapshots, а не на Git.

Основной интерфейс инфраструктуры — `REvolutionControl.exe`.

## Установка вручную

Ручная установка — fallback для разработки и диагностики. Для обычного игрока используется RE:volution Client.

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

## Лицензия

Исходный код репозитория распространяется по [MIT License](LICENSE). Сторонние материалы могут иметь отдельные условия, которые имеют приоритет для соответствующих файлов.

### Шрифты Cristalix

Шрифты получены из клиента Cristalix с сохранением исходных метаданных. Условия, ответ поддержки и область использования описаны в [docs/permissions/cristalix.md](docs/permissions/cristalix.md).
