# RE:volution Mod - Forge 1.12.2

Серверно-авторитетный игровой мод для Minecraft Forge 1.12.2. Репозиторий содержит основную игровую логику RE:volution: интерфейс игрока, руны и статусы, экономику ресурсов, managed-мобов, Realm foundation/runtime, mob-zone progression, переходы между стадиями и хранение данных игроков в MongoDB.

**Стабильная release-версия: `v1.3.12`.**

**Текущий этап разработки: следующий feature cycle после `v1.3.12`; крупные новые изменения идут в `v1.3.13+`.**

[Скачать актуальный стабильный релиз](https://github.com/Theyyk/re-volution-mod/releases/latest) · [Документация](docs/README.md) · [Архитектура](docs/architecture.md) · [Контракт v1.3.12](docs/releases/v1.3.12.md)

## Границы проекта

Этот репозиторий отвечает за gameplay-мод, который работает на dedicated-сервере и, при полном клиентском режиме, на клиенте Minecraft. Отдельные части экосистемы находятся в других репозиториях:

- **RE:volution Client** - launcher/updater, Auth API, client-only Forge mod и production manifest;
- **RE:volution Resource Pack** - визуальные ассеты рун и связанные distribution-файлы;
- **RE:volution Admin** - закрытый локальный Windows-инструмент администрирования.

Gameplay-мод не должен содержать приватные launcher-signing keys, секреты Admin API или клиентскую логику обновления.

## Текущая архитектурная база

### Realm foundation/runtime

- **Realm 0** - административный overworld / dimension `0`.
- **Realm 1** - dimension `201`, основной player Realm и эталон мира для стартовой синхронизации.
- **Realm 2** - dimension `202`.
- **Realm 3** - dimension `203`.
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
- Одна gameplay Location может содержать несколько независимых stage, адресуемых парой `locationId + mobSlotId`.
- Существование progression stage определяется `LocationDefinition`; population отвечает за наполнение stage мобами.
- Stage может быть `MOB_ZONE` или `BOSS_ZONE`.
- Финальная configured stage каждой Location автоматически является runtime boss и сохраняется как единственная `BOSS_ZONE`; при добавлении новой последней stage boss-status переносится автоматически.
- Конфигурация player Realm применяется к Realm 1/2/3, Realm 0 остаётся explicit-only.
- `RANDOM_AREA` выбирает новую независимую позицию на каждую новую физическую жизнь моба.
- Generated/random spawn surface не принимает листву/брёвна и не поднимается более чем на 3 блока выше базового Y зоны.
- `ROAMING` удерживает мобов в своей зоне, раздвигает слишком плотные группы и делает паузу примерно 1 секунду после прямого удара игрока; повторный удар перезапускает паузу.

Канонические команды stage-zone:

```text
/stage location set <locationId> <mobSlotId> <radius>
/stage location populate <locationId> <mobSlotId> <mobName> <count>
/stage location setboss <locationId> <mobSlotId>
/stage location clearboss <locationId> <mobSlotId>
/stage location here
/stage location list [locationId]
/stage location clear <locationId> <mobSlotId>
/stage location remove <locationId> <mobSlotId>
```

`location clear` удаляет population/spawn slots, но сохраняет геометрию зоны. `location remove` удаляет и population, и геометрию зоны. `setboss/clearboss` сохранены как legacy/admin-команды совместимости; обычный provisioning в `v1.3.12` поддерживает финальную Boss Zone автоматически. Старый `/custommob location ...` сохранён как legacy alias для обратной совместимости.

Клиент RE:volution визуализирует границу настроенных mob-зон тонким полупрозрачным зелёным кольцом. Кольцо следует по поверхности земли, игнорирует листву и брёвна, не изменяет блоки и отображается игрокам с установленным модом в пределах рабочей дистанции визуализации.

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

Полный контракт: [docs/releases/v1.3.7.md](docs/releases/v1.3.7.md).

### Персональная mob progression (`v1.3.8`)

Progression хранится отдельно для каждого игрока и строится поверх configured stage.

- Стадии внутри Location идут по возрастающему `mobSlotId`; номера не обязаны быть последовательными.
- Первая stage первой configured Location является стартовой точкой progression.
- Победа на текущей stage открывает следующую stage текущей Location.
- Повторная победа на уже пройденной stage не перескакивает через следующую.
- Progression credit получает только текущий `max contributor`, а не last hit.
- Физические managed-мобы остаются общими для игроков Realm, но доступ к stage персональный.
- Если stage закрыта для игрока, урон подавляется, contribution и combat pause не создаются.
- Открытые стадии сохраняются в MongoDB в коллекции `mob_progression` и не привязаны к конкретному Realm 1/2/3.
- Progression сохраняется после relog, restart сервера и смены Realm.

Исторический контракт: [docs/releases/v1.3.8.md](docs/releases/v1.3.8.md).

### Переходы между mob stages (`v1.3.9`)

`v1.3.9` добавил navigation/transition слой поверх progression, не смешивая transport с unlock-логикой.

- Для каждого stage можно задать explicit transition point через `/stage setpoint`.
- Сервер проверяет bounding box и опору destination перед телепортом.
- `/stage teleport <locationId> <mobSlotId> [player]` использует point выбранного stage и не открывает progression.
- `/stage auto <on|off> [player]` управляет trigger-zone auto-transition flag.
- Trigger-кольцо задаётся сразу для всей Location через `/stage settrigger <locationId> <offset> <radius>`.
- Trigger делится на forward/backward половины по геометрии соседних stage.
- `locked`, `forward`, `backward` сообщения задаются через `/stage setmessage`.
- Доступный trigger-переход выполняется после 20 ticks и отменяется, если игрок покидает trigger или контекст становится невалидным.
- `/stage revoke` и `/stage reset` поддерживают каскадный сброс progression.
- Клавиша `L` открывает меню Location -> stage для быстрого перехода к уже открытому stage с настроенной transition point.
- Все player teleport requests повторно валидируются сервером.

Полный контракт: [docs/releases/v1.3.9.md](docs/releases/v1.3.9.md).

### Test Foundation (`v1.3.10`)

`v1.3.10` добавил JUnit/Gradle test foundation поверх существующих standalone regression runners без изменения gameplay baseline.

- JUnit Jupiter подключён к стандартному `test`.
- Service-level тесты работают без полноценного Minecraft client/OpenGL/MongoDB там, где это возможно.
- CI публикует JUnit reports и не допускает успешный build с нулём выполненных JUnit tests.
- DEV dispatcher защищён от stable/tag маршрутизации в DEV channel.

Подробности: [docs/releases/v1.3.10.md](docs/releases/v1.3.10.md) · [docs/test-foundation.md](docs/test-foundation.md).

### Cross-Location progression и Boss Zone (`v1.3.11`, исторический контракт)

`v1.3.11` расширил progression на цепочку Location. В этой версии Boss Zone назначалась явно; начиная с `v1.3.12` финальная configured stage определяется и сохраняется как Boss Zone автоматически.

- Для завершения Location в `v1.3.11` требовалась явная единственная финальная `BOSS_ZONE`.
- Победа в Boss Zone открывает первый configured stage следующей Location.
- Пустая, но созданная stage следующей Location может быть открыта до `populate`.
- Если следующая Location отсутствует, progression корректно останавливается на текущем контенте без автогенерации.
- `/stage next` и `/stage previous` используют общий resolver и поддерживают переход через границу Location.
- Добавлен отдельный persistent `Fast-auto`, переключаемый в меню `L`; он независим от trigger-zone automation.
- Fast-auto после обычной stage ведёт на следующую stage текущей Location, а после Boss Zone - в первую stage следующей Location.
- Меню `L` получает boss-state из server snapshot: обычная stage отображается как `Моб N`, Boss Zone - как `Босс`.
- Revoke/reset и GUI/network requests остаются server-authoritative и не могут обойти progression.

Исторический контракт: [docs/releases/v1.3.11.md](docs/releases/v1.3.11.md).

### Location 1 provisioning и canonical mobs (`v1.3.12`)

`v1.3.12` закрывает большой цикл Location 1: детерминированное создание stage через REBuilder, canonical `MobL_N + population`, автоматический финальный Boss Zone и читаемый static appearance roster 1–41.

- `/location create <locationId> <mobSlotId> [x y z]` использует детерминированный plan и REBuilder templates.
- Remote provisioning поддерживает unloaded chunks, несколько Forge tickets, rollback и точный requested anchor.
- Для каждой stage используется стабильное техническое имя `Mob<locationId>_<mobSlotId>`.
- При отсутствии mob definition создаются defaults `HP=1`, reward `coins x1`; уже настроенный balance не перезаписывается.
- Новая stage получает initial population `1`; существующие Location 1 stages 1–41 reconcile пустую/тестовую population к `Mob1_N` без сброса последующих canonical count edits.
- Финальная configured stage автоматически является runtime boss и persisted `BOSS_ZONE`; предыдущие stages сохраняются как `MOB_ZONE`.
- Existing Location 1 geometry, transition points, trigger topology и world builds при reconciliation не пересоздаются.
- Appearance остаётся привязан к Location + mob slot catalog, а не к техническому имени `MobL_N`.
- Все 41 runtime slot Location 1 имеют static appearance baseline и документированный six-face UV contract; дальнейший художественный polish вариантов отложен на будущую версию.

Полный контракт: [docs/releases/v1.3.12.md](docs/releases/v1.3.12.md).

## Игровые системы

- Адаптивный GUI со статистикой, ресурсами, персонажем, рунами и навигацией.
- Multi-status foundation для рун.
- Первый встроенный статус **Яд**: 26 фиксированных позиций и 11 уникальных rune-иконок.
- Ранги рун: **Обычная -> Улучшенная -> Продвинутая**.
- Покупка рун `x1`, `x5`, `x10`, `x100` с серверной проверкой экономики.
- Дубликаты, Пыль рун, платное пробуждение и постоянные уровни усиления `+N` обрабатываются сервером.
- Усиление и ранг являются независимыми множителями; базовая формула усиления: `finalPower = basePower × (1 + enhancementLevel × enhancementStep)`.
- Три режима отображения больших чисел: упрощённый, полный и инженерный.
- Фиксированный каталог ресурсов: `coins`, `crystals`, `rune_dust`, `rune_shards`, `rune_cores`.
- Общий серверный контракт игровых ресурсов не допускает отрицательных балансов.
- Каркас 16 слотов снаряжения и пяти сборок существует, полноценные механики экипировки пока не завершены.
- Данные игроков и progression сохраняются в MongoDB; URI и имя базы задаются серверной конфигурацией.

Основной gameplay GUI открывается клавишей `C`, меню быстрого перехода - клавишей `L`. Подробнее: [игровые системы](docs/gameplay.md) · [команды](docs/commands.md).

## DEV / PROD и runtime

Основная инфраструктурная модель проекта разделяет DEV и PROD.

- **DEV** используется для development/gameplay acceptance и принимает только допустимый develop pipeline.
- **PROD** используется только для stable gameplay из `main` с release tag и проверенным server image.
- DEV и PROD используют отдельные Minecraft runtime/world и отдельные Mongo namespace.
- DEV build сам по себе не становится PROD.
- При контролируемой PROD synchronization world и Mongo заменяются DEV snapshot только после backup; это не merge данных.
- Runtime rollback опирается на сохранённые snapshots, а не на Git.

Основной пользовательский интерфейс инфраструктуры - `REvolutionControl.exe`, GUI-оболочка над orchestration backend. Обычный gameplay flow: обновить DEV, выполнить минимальный runtime smoke только для поведения, которое нельзя доказать автоматическими тестами, и принять механику.

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

Основная ветка разработки - `develop`, стабильная ветка - `main`. Изменения gameplay проходят через feature/fix ветку и PR в `develop`, затем отдельный release/promotion в `main`.

CI использует component change detection, dependency/build caches, concurrency, path filtering и exact artifact handoff, чтобы не публиковать устаревший DEV candidate поверх более нового.

CI проверяет, среди прочего:

- согласованность версии;
- release descriptions;
- JUnit и standalone regression tests;
- архитектурные guard-проверки;
- сборку Forge-мода;
- содержимое итогового JAR;
- startup/Realm orchestration contracts;
- DEV publication invariants и artifact consistency.

CI не заменяет runtime-приёмку только там, где изменение действительно зависит от реального Forge lifecycle, клиента, multiplayer или визуального поведения.

### Обязательный release gate

Перед новым стабильным релизом необходимо:

- актуализировать README и release docs;
- синхронизировать `build.gradle`, `mcmod.info` и `RevolutionMod.VERSION`;
- получить exact-main CI после promotion;
- создать release tag;
- проверить GitHub Release JAR hash/size;
- получить stable Docker image и production signed manifest;
- выполнить обязательный post-release branch audit/cleanup;
- повторно проверить remote branches;
- только после этого считать релиз `RELEASE COMPLETE`.

## Roadmap после v1.3.12

Ближайшие gameplay-механики определяются отдельно перед реализацией. На следующие циклы оставлены, в частности:

- boss HP scaling от количества игроков в Boss Zone;
- способность босса как награда;
- индивидуальные ограничения на убийство босса;
- boss-specific enter/leave hooks и отдельные respawn/reset policies;
- дальнейший visual polish Location 1 и более сильная художественная дифференциация вариантов;
- более широкий stage-centric Admin/balance tooling.

Эти пункты не являются частью v1.3.12.

## Скриншоты

Скриншоты предыдущего GUI baseline сохранены в [docs/screenshots/v1.2.12](docs/screenshots/v1.2.12/README.md), исторические изображения - в [docs/archive/screenshots](docs/archive/screenshots/README.md). Они являются снимками соответствующих версий.

## Документация

[docs/README.md](docs/README.md) является оглавлением технической документации. Версионные заметки хранятся в `docs/releases/`, а опубликованные артефакты и пользовательские release notes - в [GitHub Releases](https://github.com/Theyyk/re-volution-mod/releases).

README описывает текущее состояние проекта. Версионные документы фиксируют состояние конкретного релиза и не переписываются под будущие функции.

## Лицензия

Исходный код репозитория распространяется по [MIT License](LICENSE). Сторонние материалы могут иметь отдельные условия, которые имеют приоритет для соответствующих файлов.

### Шрифты Cristalix

Шрифты получены из клиента Cristalix с сохранением исходных метаданных. Условия, ответ поддержки и область использования описаны в [docs/permissions/cristalix.md](docs/permissions/cristalix.md).

CI scheduling, cache, concurrency and artifact policy: [README-CI.md](README-CI.md).
