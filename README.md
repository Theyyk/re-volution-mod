# RE:volution Mod - Forge 1.12.2

Серверно-авторитетный игровой мод для Minecraft Forge 1.12.2. Репозиторий содержит основную игровую логику RE:volution: интерфейс игрока, руны и статусы, экономику ресурсов, managed-мобов, Realm foundation/runtime, mob-zone progression, переходы между стадиями и хранение данных игроков в MongoDB.

**Стабильная release-версия: `v1.3.10`.**

**`v1.3.10 Test Foundation` закрепляет JUnit/Gradle baseline: 22 JUnit-теста поверх 30 существующих regression tasks без изменения gameplay-контракта v1.3.9. [Аудит и запуск тестов](docs/test-foundation.md).**

[Скачать актуальный стабильный релиз](https://github.com/Theyyk/re-volution-mod/releases/latest) · [Документация](docs/README.md) · [Архитектура](docs/architecture.md) · [Контракт v1.3.10](docs/releases/v1.3.10.md)

## Границы проекта

Этот репозиторий отвечает за gameplay-мод, dedicated-серверную логику, клиентские Forge-компоненты gameplay UI, managed-мобов, Realms, progression и переходы. Launcher/Auth, Resource Pack и Admin развиваются отдельно. Секреты launcher signing и Admin API в gameplay-репозиторий не входят.

## Stable gameplay baseline

`v1.3.10` не добавляет новую gameplay-механику относительно `v1.3.9`. Стабильный gameplay baseline включает:

- Realm 0 как admin overworld и player Realms 1/2/3 в dimensions 201/202/203;
- managed mob-zones и `/stage location ...` как канонический namespace;
- contribution/rewards/HP rollback;
- персональную progression по `locationId + mobSlotId`;
- transition points, `next/previous`, `/stage teleport`, per-player `/stage auto`;
- location-wide trigger geometry и `locked/forward/backward` messages;
- каскадные `/stage revoke` и `/stage reset`;
- mob-zone/transition overlay;
- меню быстрого перехода по клавише `L` с серверной revalidation progression/safety.

Полный gameplay-контракт переходов: [v1.3.9](docs/releases/v1.3.9.md).

## Test Foundation v1.3.10

- JUnit Jupiter `5.10.3` выполняется стандартной Gradle-задачей `test`.
- 22 JUnit-теста покрывают progression service, transition service и trigger service.
- Все 30 прежних standalone regression runners остаются частью `check/build`.
- При нулевом наборе JUnit сборка завершается ошибкой.
- CI публикует XML/HTML JUnit reports отдельным artifact.
- Test fixtures не входят в gameplay JAR.
- DEV dispatcher принимает только successful push ветки `develop`; stable tags и feature/fix branches не публикуются в DEV автоматически.

Подробности: [docs/test-foundation.md](docs/test-foundation.md) и [docs/dev-dispatcher.md](docs/dev-dispatcher.md).

## DEV / PROD

- `develop` является единственным source branch автоматической DEV publication chain.
- `main` и annotated `vX.Y.Z` tag используются для stable release.
- DEV и PROD используют отдельные Minecraft world/runtime и Mongo namespace.
- DEV build сам по себе не становится PROD.
- DEV→PROD synchronization является явной replacement-операцией с backup, а не merge.
- Runtime rollback использует snapshots и предыдущий image, а не Git rollback мира/БД.

`REvolutionControl.exe` остаётся основным GUI-инструментом локального управления runtime. CI не заменяет Forge/runtime acceptance там, где изменение зависит от реального сервера, клиента, multiplayer, collision или MongoDB.

## Разработка и release gate

Обычный workflow: feature/fix branch → PR → ordinary merge в `develop` → exact develop CI/DEV acceptance → release branch → PR в `main` → exact-main CI → annotated tag → release pipeline.

Перед каждым stable release обязательны:

- согласованные version metadata;
- актуальные README и русские release docs;
- английские GitHub Release notes;
- exact-head/exact-main CI;
- проверка итогового JAR;
- stable Docker image и Client production manifest.

Force/squash/rebase не являются стандартным merge flow проекта.

## Тесты

Java 8 и Gradle wrapper:

```powershell
.\gradlew.bat '-Dnet.minecraftforge.gradle.check.certs=false' test
.\gradlew.bat '-Dnet.minecraftforge.gradle.check.certs=false' clean build
```

`test` выполняет 22 JUnit-теста. `check/build` дополнительно запускают 30 standalone regression runners и architecture/security guards.

## Runtime ограничения Test Foundation

JUnit не доказывает реальный teleport, collision/support, player tick, delayed trigger execution, transport или MongoDB integration. Эти сценарии принимаются отдельно в DEV/runtime при изменениях соответствующего gameplay-кода.

## Roadmap после v1.3.10

Следующая gameplay-итерация планируется как `v1.3.11`:

1. переходы между разными Location поверх уже принятой in-Location progression/transition модели;
2. правила Location/boss unlock без смешивания с `mobSlot` progression;
3. затем UX переходов и дальнейшее Admin/UI управление Location/stage;
4. scaling и нагрузочная оптимизация остаются отдельным этапом и не считаются подтверждённой вместимостью текущего runtime.

## Документация

- [Оглавление](docs/README.md)
- [Команды](docs/commands.md)
- [Архитектура](docs/architecture.md)
- [Разработка и проверки](docs/development.md)
- [Русские описания релизов](docs/releases/README.md)
- [Test Foundation](docs/test-foundation.md)

## Установка

Для обычного игрока используется RE:volution Client. Ручная установка остаётся fallback для разработки: Java 8, Forge 1.12.2/14.23.5.2859 и актуальный `RE-volution-<version>.jar` из GitHub Releases.

## Лицензия

Исходный код распространяется по [MIT License](LICENSE). Сторонние материалы могут иметь отдельные условия.
