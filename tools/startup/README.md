# Локальный запуск с постоянным Realm 1

`build-server-run.ps1` — версия существующего внешнего скрипта для размещения
в `E:\MyProject\RE-volution\Script`, рядом с текущими `common.ps1` и
`world-sync.ps1`. Сам по себе из Git checkout он не запускается: эти два файла
остаются частью локального окружения. `preflight-and-run.ps1`, `start-server.ps1`
и `build-test-run.ps1` уже вызывают этот скрипт и изменения не требуют.

Скрипт собирает проект, заданный `$Project` в `common.ps1`. Когда исходники этого
проекта содержат `realm/world/RealmStartupSync.java`, он сохраняет текущее
Test/world. После запуска JAR сам Forge заменяет только DIM202/203 из DIM201.
Подготовка `-PrepareOnly` собирает/устанавливает JAR без замены мира и без sync;
`-CheckOnly` выполняет проверки без изменения миров и без запуска Forge.

Для прежних веток без RealmStartupSync сохранены существующая проверка Build
и полное обновление Build/world → Test/world. Перед тестом Realm нужно выбрать
нужную feature-ветку именно в `$Project`; одного ручного копирования JAR
недостаточно, потому что launcher собирает этот проект заново. Не переключать
ветку проекта во время сборки/запуска.

Перед установкой сохранить backup прежнего build-server-run.ps1. Обновление
самого скрипта не запускает сервер, не меняет миры и не устанавливает JAR.
`Test-RealmLauncher.ps1` создаёт изолированное окружение с подставными сборкой,
проверками и запуском. Он проверяет три режима для новой и старой ветки, а также
сохранность Realm 1 и overworld. Тест не использует реальные серверы, Minecraft,
MongoDB или пользовательскую папку клиента; временный APPDATA восстанавливается.

Запуск проверки в Windows PowerShell:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\startup\Test-RealmLauncher.ps1
```
