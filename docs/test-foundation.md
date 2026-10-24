# v1.3.10 Test Foundation

Baseline: annotated tag `v1.3.9`, commit `3641816250e7a24bb7d942b01bb21a0267359e98`.

`v1.3.10` оформляет Test Foundation как отдельный технический release. Gameplay behavior сохраняет baseline `v1.3.9`; production-код gameplay не менялся, кроме version metadata.

## Аудит исходного baseline

- `src/test/java` уже существовал и зеркалировал production packages.
- 30 Java assertion runners были зарегистрированы в `regressionTests` и входили в `check/build`.
- Уже существовало покрытие progression ordering/gating, transition points, trigger geometry, Realm/runtime, ресурсов, рун, network/persistence contracts и Admin API.
- До Test Foundation стандартный Gradle `test` не являлся доказательством выполнения JUnit.

## Что добавлено

1. JUnit Jupiter `5.10.3` и platform launcher `1.10.3`, `useJUnitPlatform()` и fail при нулевом наборе тестов.
2. 9 JUnit-тестов `MobProgressionService` для failure/retry, idempotency, cascading cache invalidation, isolation, configuration filtering и clear/reload.
3. 6 JUnit-тестов `StageTransitionService` для repository persistence, validation, overlaps, legacy geometry, deterministic ties и invalid entries.
4. 7 JUnit-тестов `StageTransitionTriggerService` для safe reconfigure, selective message clear, invalid inputs, persistence failure, reload/clear и stage direction axis.
5. Два in-memory repository fixtures, возвращающих detached collections, чтобы cache tests не проходили случайно из-за общих ссылок.
6. CI artifact `junit-reports-<run_id>-<attempt>` с XML/HTML reports, не считающийся gameplay artifact для Docker dispatcher.

Всего добавлено **22 JUnit-теста**, при этом все **30 прежних regression runners** сохранены.

## Запуск

Используйте Java 8 и Gradle wrapper:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk8u504-b01'
.\gradlew.bat '-Dnet.minecraftforge.gradle.check.certs=false' test
.\gradlew.bat '-Dnet.minecraftforge.gradle.check.certs=false' clean build
.\gradlew.bat '-Dnet.minecraftforge.gradle.check.certs=false' test --tests '*MobProgressionServiceJUnitTest'
python .github/scripts/check-structure.py build/libs/RE-volution-1.3.10.jar
```

HTML report: `build/reports/tests/test/index.html`.
XML: `build/test-results/test/TEST-*.xml`.

## DEV routing regression

DEV dispatcher принимает только successful `push` ветки `develop` этого репозитория. Stable tags, main, feature/fix, PR, manual/release events и forks не отправляют `mod-dev-build`. Отдельный regression test использует payload исторического tag-run `v1.3.9` и проверяет, что он не проходит DEV routing.

## Границы покрытия

JUnit-набор не запускает Minecraft, Forge dedicated server, OpenGL или MongoDB. Реальный teleport, collision/support, player tick, delayed trigger execution, transport и persistence integration остаются отдельной runtime/integration-приёмкой.

Test Foundation не меняет gameplay, schema или economy. Его цель - закрепить критичную business/service логику перед следующими gameplay-итерациями.
