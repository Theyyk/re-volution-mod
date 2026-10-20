# Запуск и ручное копирование Realm 0

Скрипты размещаются в `E:\MyProject\RE-volution\Script` рядом с common.ps1.
Два ручных copy-скрипта размещены в подпапке `World-menegment`; общие файлы
common.ps1, world-sync.ps1 и realm-world-sync.ps1 остаются в родительской Script.
Copy-скрипты поддерживают и прежнее размещение рядом с общими файлами.

| Скрипт | Действие |
|---|---|
| preflight-and-run.ps1 | Обычный запуск Test: Realm 0/1 сохраняются; Forge до загрузки worlds заменяет 2/3 копиями 1 |
| copy-build-realm0-to-test.ps1 | Ручная замена Test Realm 0 из Build; Realm 1–3 Test сохраняются; сервер не запускается |
| copy-test-realm0-to-build.ps1 | Ручная замена Build Realm 0 из Test; Realm 1–3 Test не читаются/не импортируются; сервер не запускается |

## Обычный запуск

preflight-and-run.ps1 сначала выполняет CheckOnly, затем build-server-run.ps1:
подготовку Auth/resource pack, сборку `$Project`, установку JAR сервера/клиента
и запуск Forge. Мир Build не проверяется и не копируется; он может работать.
Test должен быть остановлен. Realm 0 сохраняет свои изменения между запусками;
Realm 1 остаётся эталоном, а изменения Realm 2/3 сбрасываются при старте Forge.

Project в common.ps1 должен содержать RealmStartupSync.java. Ветка без него
блокируется, автоматического возврата к полному Build→Test копированию нет.
PrepareOnly собирает/устанавливает JAR без запуска и без sync. CheckOnly не
собирает/не устанавливает JAR, не запускает сервисы/Forge и не меняет миры.

## Ручные операции

Оба copy-скрипта поддерживают CheckOnly без копирования, сборки, запуска серверов
и команд save-off/save-on. Обычное выполнение подготавливает отдельный world,
проверяет файлы, сохраняет прежний мир назначения в Script/WorldBackups и только
затем публикует копию. Ошибка копирования сохраняет прежнюю цель, ошибка
публикации пытается восстановить её из backup. Блокировки исключают параллельный
runtime-запуск и конфликтующие операции копирования.

Из источника берутся только overworld region/data и корневые файлы уровня.
Все папки DIM*, playerdata/stats/advancements берутся из назначения. session.lock
и .revolution-* не копируются. Поэтому DIM201/202/203 и данные игроков назначения
сохраняются; MongoDB, mods и server config не переносятся.
level.dat, seed/time/game rules остаются общими метаданными Forge dimensions.

### Build → Test Realm 0

Test должен быть остановлен. Если Build работает, нужны включённый RCON,
rcon.port и пароль в server.properties Build. Выполняются save-off → save-all
flush → проверенное копирование overworld → save-on. Игроки Build могут
продолжать играть; в снимок входят данные на момент flush. save-on вызывается
также при ошибках, а при ошибке самого save-on повторяется попытка восстановления;
цель при этом не публикуется. Если Build остановлен, используется offline-копия
без RCON. Папки DIM* Build не импортируются ни в одном режиме.

### Test Realm 0 → Build

Оба сервера должны быть остановлены. Копируется только overworld Test;
Realm 1/2/3 Test не изменяются и не переносятся в Build. Собственные dimensions
и данные игроков Build сохраняются. Это отдельная повторяемая операция;
старый одноразовый promote-test-world-to-build.ps1 для неё не используется.

## Проверки

Test-RealmLauncher.ps1 проверяет отсутствие копирования в обычном запуске,
CheckOnly/PrepareOnly и отдельные entry points. Test-RealmOverworldSync.ps1
реально копирует временные миры в обоих направлениях, проверяет сохранность
dimensions/player data, backup/rollback, блокировки и имитирует RCON для
работающего Build с успешным копированием и ошибками flush/copy/save-on.
В тестах не запускаются реальные серверы, Minecraft или MongoDB.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\startup\Test-RealmLauncher.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\startup\Test-RealmOverworldSync.ps1
```
