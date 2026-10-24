# v1.3.10 Test Foundation

Baseline: annotated tag `v1.3.9`, commit
`3641816250e7a24bb7d942b01bb21a0267359e98`.

Ветка начинается от этого stable commit. Исправление DEV dispatcher переносится
обычным merge из main до добавления тестов. Нового stable release этот этап
не создаёт: metadata мода остаётся `1.3.9` до отдельного release gate v1.3.10.

## Аудит исходного baseline

- `src/test/java` существует; пакеты зеркалируют production.
- 30 Java assertion runner'ов зарегистрированы в `regressionTests` в `build.gradle`.
  Каждый — отдельная задача `JavaExec`, все входят в `check` и `build`.
- `MobProgressionTest` уже проверяет сортировку и gating, разреженную цепочку
  слотов, next/previous navigation и успешный каскад revoke/reset.
- `StageTransitionServiceTest` проверяет transition point, допустимость координат
  вне mob-zone, очистку точки, nearest lookup и отбрасывание NaN.
- `StageTransitionTriggerDefinitionTest` проверяет границы кольца offset/radius,
  деление 180/180, конфигурируемые сообщения и режим, некорректную геометрию.
- Есть отдельные regression-проверки рун, экономики, ресурсов, combat, wire
  protocol, persistence contracts, realm startup, whitelist, admin API и GUI.
- Python guards проверяют архитектуру, структуру, версии и release notes;
  PowerShell-тесты startup работают с временными fixtures.
- В исходной v1.3.9 JUnit-зависимостей и `useJUnitPlatform()` нет. Задача `test` не является
  доказательством исполнения unit-тестов; успешные JavaExec-задачи — являются.
- CI уже запускает `clean build`, проверяет содержимое JAR и загружает artifact.

## Что добавлено

1. JUnit Jupiter `5.10.3` и platform launcher `1.10.3` подключены к Gradle
   `test` через `useJUnitPlatform()`. Java 8 и все 30 runners сохранены.
2. 9 JUnit-тестов progression service: отказ записи unlock/revoke/reset, повторный unlock,
   инвалидирование уже прогретого кеша при каскаде, изоляция игроков и ранних
   Locations, фильтрация disabled/missing spawn, очистка/повторная загрузка.
3. 6 JUnit-тестов transition service: конфигурация и очистка через in-memory repository,
   сохранение геометрии зоны, отказ до записи, overlap и детерминированные ties.
4. 7 JUnit-тестов trigger service: повторная настройка без потери сообщений, выборочное
   изменение/очистка, reload/clear, ошибка сохранения, ось соседних stages.
5. CI сохраняет XML/HTML test reports даже после ошибки сборки. Gradle печатает
   число тестов и завершает проверку ошибкой при нулевом наборе JUnit.

Всего **22 новых теста**, без копирования 30 существующих runner'ов в JUnit.
Два общих in-memory repository fixture лежат в зеркальных test-пакетах
`location.repository` и `mob.repository`; методы чтения возвращают отдельные
коллекции, чтобы тесты инвалидирования кеша не проходили из-за общих ссылок.

## Запуск и отчёты

Используйте JDK 8 и wrapper. В PowerShell:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk8u504-b01'
.\gradlew.bat '-Dnet.minecraftforge.gradle.check.certs=false' test
.\gradlew.bat '-Dnet.minecraftforge.gradle.check.certs=false' clean build
.\gradlew.bat '-Dnet.minecraftforge.gradle.check.certs=false' test --tests '*MobProgressionServiceJUnitTest'
python .github/scripts/check-structure.py build/libs/RE-volution-1.3.9.jar
```

`test` выполняет JUnit; `check`/`build` — JUnit, все прежние JavaExec regression
tasks и архитектурные guards. Для отдельного прежнего сценария его Gradle task
по-прежнему доступна, например `mobProgressionRegression`.

HTML: `build/reports/tests/test/index.html`; XML: `build/test-results/test/TEST-*.xml`.
В CI они находятся в artifact `junit-reports-<run_id>-<attempt>` (7 дней).
Имя отчёта не начинается с `RE-volution-`, поэтому он не считается gameplay
artifact для Docker dispatcher. JUnit и test fixtures не входят в gameplay JAR.

Локально на копии stable baseline подтверждены: `clean build`, 30 старых
runner'ов, 22 JUnit-теста (0 failures, 0 skipped), packaged guard. Это отдельное
свидетельство; результат CI фиксируется для точного SHA ветки и после merge.
Негативная проверка с исключёнными JUnit-классами завершилась ожидаемой ошибкой
`No JUnit tests executed`; после неё обычный набор выполнен повторно.

## Границы

Новые проверки используют DTO и in-memory repository ports, не стартуют
Minecraft, Forge server, OpenGL или MongoDB. Реальный teleport, collision/support,
player tick, delayed trigger execution, transport и persistence integration
остаются отдельной runtime/integration-приёмкой. Покрытие этих путей не заявляется.

Production-код не изменён для перечисленных service-тестов: публичные
методы и существующие package-private pure helpers уже позволяют их написать.
Новая gameplay feature, смена экономики/схемы, PROD runtime и DEV→PROD sync
не входят в этот этап.
