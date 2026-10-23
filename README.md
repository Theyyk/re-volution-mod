# RE:volution Mod — Forge 1.12.2

Серверно-авторитетный игровой мод для Minecraft Forge 1.12.2. Репозиторий содержит основную игровую логику RE:volution: интерфейс игрока, руны и статусы, экономику ресурсов, managed-мобов, Realm foundation/runtime, mob-zone progression и хранение данных игроков в MongoDB.

**Стабильная release-версия: `v1.3.8`.**

**Следующий этап разработки: `v1.3.9` - переходы между mob stages внутри одной Location. Разработка `v1.3.9` ещё не является release candidate.**

[Скачать актуальный стабильный релиз](https://github.com/Theyyk/re-volution-mod/releases/latest) · [Документация](docs/README.md) · [Архитектура](docs/architecture.md) · [Контракт v1.3.8](docs/releases/v1.3.8.md)

## Границы проекта

Этот репозиторий отвечает за gameplay-мод, который работает на dedicated-сервере и, при полном клиентском режиме, на клиенте Minecraft. Отдельные части экосистемы находятся в других репозиториях:

- **RE:volution Client** — launcher/updater, Auth API, client-only Forge mod и production manifest;
- **RE:volution Resource Pack** — визуальные ассеты рун и связанные distribution-файлы;
- **RE:volution Admin** — закрытый локальный Windows-инструмент администрирования.

Gameplay-мод не должен содержать приватные launcher-signing keys, секреты Admin API или клиентскую логику обновления.

## Текущая архитектурная база

### Realm foundation/runtime

- **Realm 0** — административный overworld / dimension `0`.
- **Realm 1** — dimension `201`, основной player Realm и эталон мира для стартовой синхронизации.
- **Realm 2** — dimension `202`.
- **Realm 3** — dimension `203`.
- При обычном входе игрок назначается в наименее населённый Realm из `1..3`; Realm 0 в автоматическом распределении не участвует.
- При равной минимальной нагрузке выбор выполняется случайно среди Realm с одинаковым минимумом.
- `/realm join <realmId> [player]` остаётся административной командой и поддерживает Realm 0.
- На каждом старте dedicated-сервера `DIM202` и `DIM203` полностью подготавливаются из сохранённого `DIM201` до загрузки миров Forge. Во время работы сервера Realm 1/2/3 независимы.
- Realm 0 не входит в startup sync.
- Runtime managed-мобов не должен принудительно загружать пустые Realm-измерения.
- Realm ID, Dimension ID, Location ID, Mob Slot ID / Spawn ID и UUID сущности являются разными идентификаторами.

Подробный контракт: [docs/realm-startup-sync.md](docs/realm-startup-sync.md).

### Location / Mob Position foundation и managed-мобы

Runtime managed-мобов использует Location/Mob Spawn модель поверх логических определений мобов.

- Один Mob Spawn соответствует одной физической сущности.
- Несколько spawn-slot могут использовать один и тот же логический `mobId`.
- Поддерживаются `FIXED_POINT` и `RANDOM_AREA`.
- Поддерживаются `STATIONARY` и `ROAMING`.
- Обычный flow `location populate` создаёт `ROAMING`-мобов и делит population примерно 50/50 между fixed и random slots.
- Одна gameplay Location может содержать несколько независимых mob-зон, адресуемых парой `locationId + mobSlotId`.
- Конфигурация player Realm применяется к Realm 1/2/3, Realm 0 остаётся explicit-only.
- `RANDOM_AREA` выбирает новую независимую позицию на каждую новую физическую жизнь моба.
- Generated/random spawn surface не принимает листву/брёвна и не поднимается более чем на 3 блока выше базового Y зоны.
- `ROAMING` удерживает мобов в своей зоне, раздвигает слишком плотные группы и делает паузу примерно 1 секунду после прямого удара игрока; повторный удар перезапускает паузу.

Основные команды mob-zone:

```text
/custommob location set <locationId> <mobSlotId> <radius>
/custommob location populate <locationId> <mobSlotId> <mobName> <count>
/custommob location here
/custommob location list <locationId>
/custommob location clear <locationId> <mobSlotId>
/custommob location remove <locationId> <mobSlotId>
```

`location clear` удаляет population/spawn slots, но сохраняет геометрию зоны. `location remove` удаляет и population, и геометрию зоны.

Клиент RE:volution визуализирует границу настроенных mob-зон тонким полупрозрачным зелёным кольцом. Кольцо следует по поверхности земли, игнорирует листву и брёвна, не изменяет блоки и отображается всем игрокам с установленным модом в пределах рабочей дистанции визуализации.

### Contribution, rewards и HP rollback (`v1.3.7`)

Для каждого физического managed-моба в mob-zone ведётся отдельное runtime-состояние вклада игроков.

- Contribution учитывает только фактически снятый HP. Overkill ограничивается оставшимся HP моба.
- Вклад принадлежит конкретной физической жизни моба и не переносится на новый respawn.
- Если игрок перестаёт быть участником текущей mob-zone, его contribution удаляется, а мобу возвращается ровно нанесённый этим игроком урон, но не выше max HP.
- После возвращения игрок начинает contribution заново с нуля.
- После смерти награда делится между актуальными участниками пропорционально их effective damage.
- Rune reward bonus применяется отдельно к уже вычисленной доле конкретного игрока.
- Определяется max contributor для progression-логики; при точной ничьей выигрывает игрок, который раньше достиг итогового равного значения contribution.
- Respawn и unload/load физического mob-slot очищают contribution state и восстанавливают полный HP.
- Legacy Realm 0 managed-мобы без Location/Mob Spawn сохраняют прежнюю last-hit reward семантику.

Runtime-приёмка contribution/reward foundation, включая multiplayer/max-contributor regression, подтверждена в ходе подготовки `v1.3.8`.

Полный контракт: [docs/releases/v1.3.7.md](docs/releases/v1.3.7.md).

### Персональная mob progression (`v1.3.8`)

Progression строится отдельно для каждого world Location ID поверх заполненных mob-zone.

- В progression chain входят только существующие заполненные mob-zone.
- Стадии идут по возрастающему `mobSlotId`; номера не обязаны быть последовательными.
- Первый заполненный mobSlot открыт игроку автоматически.
- Победа на текущей стадии открывает ровно следующий заполненный mobSlot этой Location.
- Повторная победа на уже пройденной стадии не перескакивает через следующую.
- Progression credit получает только текущий `max contributor`, а не last hit.
- Физические managed-мобы остаются общими для игроков Realm, но доступ к стадии персональный.
- Если stage закрыт для игрока, урон подавляется, contribution и combat pause не создаются.
- Открытые стадии сохраняются в MongoDB в коллекции `mob_progression` по `playerUuid + locationId` и не привязаны к конкретному Realm 1/2/3.
- Progression сохраняется после relog, restart сервера и смены Realm.
- Legacy Realm 0 и конфигурации вне обычной mob-zone progression сохраняют совместимое поведение.

Runtime-приёмка `v1.3.8` завершена: проверены последовательный unlock, locked damage gate, отсутствие перескока стадий, persistence после выхода/restart, сохранение между Realm, разные progression state игроков и max-contributor flow.

Полный контракт и checklist: [docs/releases/v1.3.8.md](docs/releases/v1.3.8.md).

## Игровые системы

- Адаптивный GUI со статистикой, ресурсами, персонажем, рунами и навигацией.
- Multi-status foundation для рун.
- Первый встроенный статус **Яд**: 26 фиксированных позиций и 11 уникальных rune-иконок.
- Ранги рун: **Обычная → Улучшенная → Продвинутая**.
- Покупка рун `x1`, `x5`, `x10`, `x100` с серверной проверкой экономики.
- Дубликаты, Пыль рун, платное пробуждение и постоянные уровни усиления `+N` обрабатываются сервером.
- Усиление и ранг являются независимыми множителями; базовая формула усиления: `finalPower = basePower × (1 + enhancementLevel × enhancementStep)`.
- Три режима отображения больших чисел: упрощённый, полный и инженерный.
- Фиксированный каталог ресурсов: `coins`, `crystals`, `rune_dust`, `rune_shards`, `rune_cores`.
- Общий серверный контракт игровых ресурсов не допускает отрицательных балансов.
- Каркас 16 слотов снаряжения и пяти сборок существует, полноценные механики экипировки пока не завершены.
- Данные игроков и progression сохраняются в MongoDB; URI и имя базы задаются серверной конфигурацией.

Интерфейс gameplay-мода открывается клавишей `C`. Подробнее: [игровые системы](docs/gameplay.md) · [команды](docs/commands.md).

## DEV / PROD и runtime

Основная инфраструктурная модель проекта разделяет DEV и PROD.

- **DEV** используется для development/gameplay acceptance и может принимать допустимые development-ветки.
- **PROD** используется только для stable gameplay из `main` с annotated release tag и проверенным server image.
- DEV и PROD используют отдельные Minecraft runtime/world и отдельные Mongo namespace.
- DEV build сам по себе не становится PROD.
- При контролируемой PROD synchronization world и Mongo заменяются DEV snapshot только после backup; это не merge данных.
- Runtime rollback опирается на сохранённые snapshots, а не на Git.

Основной пользовательский интерфейс инфраструктуры — `REvolutionControl.exe`, GUI-оболочка над orchestration backend. Обычный gameplay flow: обновить DEV, выполнить Smoke Check, запустить DEV client и проверить механику. PowerShell остаётся backend/debug fallback.

Внутри dedicated Forge сохраняется Realm startup contract: до загрузки миров `DIM202` и `DIM203` подготавливаются из сохранённого `DIM201`; во время runtime Realm независимы.

## Установка вручную

Ручная установка является fallback для разработки и диагностики. Для обычного игрока используется RE:volution Client.

1. Установите Java 8 и Minecraft Forge **1.12.2 / 14.23.5.2859**.
2. Скачайте `RE-volution-<version>.jar` из [актуального GitHub Release](https://github.com/Theyyk/re-volution-mod/releases/latest).
3. Поместите JAR в `mods` dedicated-сервера и клиента, если используется полный modded-client режим. Старую версию JAR удалите.
4. Настройте MongoDB и серверную конфигурацию.
5. Для GUI рун установите официальный RE:volution Resource Pack.

Полная настройка: [docs/setup.md](docs/setup.md).

## Resource Pack

Актуальные rune-ассеты находятся в [RE:volution Resource Pack](https://github.com/Theyyk/re-volution-resource-pack/releases/latest). Resource Pack отвечает за визуальные ресурсы; состояние, геометрия GUI и игровая логика находятся в моде.

Текущий namespace rune-ассетов Resource Pack: `customguimod`. При отсутствии нужной текстуры gameplay GUI использует безопасный vanilla fallback.

## Разработка и проверки

Основная ветка разработки — `develop`, стабильная ветка — `main`. Изменения gameplay обычно проходят через feature-ветку и PR в `develop`, затем release/promotion в `main`.

CI использует component change detection, dependency/build caches, concurrency, path filtering и exact artifact handoff, чтобы не запускать тяжёлые jobs без необходимости и не публиковать устаревший DEV candidate поверх более нового.

CI проверяет, среди прочего:

- согласованность версии;
- release descriptions;
- архитектурные guard-проверки;
- сборку Forge-мода;
- содержимое итогового JAR;
- startup/Realm orchestration contracts;
- DEV publication invariants и artifact consistency.

CI не заменяет runtime-приёмку там, где изменение зависит от реального Forge lifecycle, клиента, multiplayer или сохранённых миров.

### Обязательный release gate для README

`README.md` является частью release contract и обязан быть актуализирован до или в составе release/promotion PR.

Перед каждым новым стабильным релизом необходимо проверить и при необходимости обновить:

- текущую stable-версию и статус следующего этапа разработки;
- фактически доступные gameplay-возможности;
- публичные команды и их актуальную семантику;
- текущую архитектурную базу и runtime-ограничения;
- roadmap и следующий незавершённый этап.

Promotion `develop -> main` не считается полностью подготовленным к релизу, если README описывает старую модель или уже изменившееся поведение.

## Roadmap после v1.3.8

1. `v1.3.9` — mob-stage transition foundation внутри одной Location: progression остаётся привязана к `mobSlot`, а переход использует состояние progression для выбора следующей доступной стадии;
2. основной режим v1.3.9 — ручной переход через transition zone у края/в чаще Location с плавным переносом к следующей стадии;
3. заложить переключаемый `autoTransitionEnabled` как функцию перехода игрока, без отдельного transition-флага на каждом `mobSlot`; автоматический переход включать отдельным последующим этапом;
4. scaling и дальнейшая оптимизация под большую нагрузку выполняются отдельным этапом после transition foundation и не считаются уже подтверждённой вместимостью.

## Скриншоты

Скриншоты предыдущего GUI baseline сохранены в [docs/screenshots/v1.2.12](docs/screenshots/v1.2.12/README.md), исторические изображения — в [docs/archive/screenshots](docs/archive/screenshots/README.md). Они являются снимками соответствующих версий.

## Документация

[docs/README.md](docs/README.md) является оглавлением технической документации. Версионные заметки хранятся в `docs/releases/`, а опубликованные артефакты и пользовательские release notes — в [GitHub Releases](https://github.com/Theyyk/re-volution-mod/releases).

README описывает текущее состояние проекта. Версионные документы фиксируют состояние конкретного релиза и не переписываются под будущие функции.

## Лицензия

Исходный код репозитория распространяется по [MIT License](LICENSE). Сторонние материалы могут иметь отдельные условия, которые имеют приоритет для соответствующих файлов.

### Шрифты Cristalix

Шрифты получены из клиента Cristalix с сохранением исходных метаданных. Условия, ответ поддержки и область использования описаны в [docs/permissions/cristalix.md](docs/permissions/cristalix.md).

CI scheduling, cache, concurrency and artifact policy: [README-CI.md](README-CI.md).
