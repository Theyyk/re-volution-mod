# Стандартный runtime-запуск Realm

Точка входа остаётся `E:\MyProject\RE-volution\Script\preflight-and-run.ps1`.
Она сначала вызывает проверку, затем обычный запуск через build-server-run.ps1.
Файлы этого каталога устанавливаются рядом с существующими common.ps1 и
preflight-and-run.ps1. world-sync.ps1 содержит прежние функции копирования/backup;
realm-world-sync.ps1 добавляет сборку составного мира.

Для проекта с RealmStartupSync.java обычный запуск выполняет:

1. Проверку остановленных Build/Test, структуры world, Auth и resource pack.
2. Подготовку сервисов, сборку `$Project`, установку JAR сервера и клиента.
3. Подготовку отдельного world: overworld из Build, папки DIM* и общие
   playerdata/stats/advancements из текущего Test. Папки DIM* и данные игроков
   Build никогда не импортируются. session.lock и .revolution-* не копируются.
4. Проверку содержимого, backup прежнего Test/world и замену подготовленным миром.
5. Запуск Forge: до загрузки dimensions он копирует Test DIM201 в DIM202/DIM203.

Realm 0 обновляется из Build, Realm 1 сохраняется из Test, 2/3 сбрасываются по
Realm 1. MongoDB, mods и server config из Build не копируются. Overworld включает
region, data и корневые файлы, в том числе level.dat; метаданные уровня
(seed/time/game rules) по-прежнему общие для Forge dimensions. Содержимое DIM201
сохраняется; независимые метаданные уровня для каждого Realm не вводятся.

Build должен быть остановлен: открытые world-файлы не копируются даже через
save-off. Запуск Build и другие операции Test блокируются на время подготовки.
Ошибка копирования оставляет текущий Test/world, ошибка публикации пытается
вернуть прежний мир из backup. Отсутствие DIM201 при существующих 2/3 и
незавершённая Realm-транзакция блокируют обновление ещё до копирования.

`-CheckOnly` не собирает/не устанавливает JAR, не запускает сервисы/Forge и не
меняет миры. `-PrepareOnly` для Realm-ветки собирает/устанавливает JAR без
обновления overworld и без Realm sync. Прежние ветки без RealmStartupSync
сохраняют исторический полный Build→Test workflow.

Launcher собирает проект, заданный `$Project` в common.ps1. Для runtime 1.3.4
нужна feature/v1.3.4-realm-foundation именно в этом проекте. Не переключать
ветку во время сборки/запуска. Обновление скриптов само по себе сервер не запускает.

Проверки Windows PowerShell без реальных серверов или MongoDB:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\startup\Test-RealmLauncher.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\startup\Test-RealmOverworldSync.ps1
```

Первый тест проверяет вызовы и режимы launcher. Второй реально копирует временные
миры, проверяет Realm 0 из Build и сохранность Test Realm 1/других dimensions/
player data на двух запусках, backup, блокировки и отказы/rollback. Закрытый для
чтения файл внутри Build DIM201 доказывает, что Build Realms не читаются.
Проверки включены в Windows CI; Java sync проверяется в Linux CI.
