# Команды

Названия некоторых команд сохраняют прежнюю терминологию для обратной совместимости.

### Realm

```text
/realm join <realmId> [player]
```

Административная команда перехода в игровой Realm 1, 2 или 3. Realm 0 - прежний административный overworld/dimension 0, в sync не участвует. Команды `/realm sync` нет: Realm 2/3 полностью заменяются из Realm 1 только при старте выделенного сервера до загрузки dimensions. [Контракт и проверки](realm-startup-sync.md).

### Stage / Location

Каноническая группа настройки Location и mob-zone находится в `/stage location`:

```text
/stage location set <locationId> <mobSlotId> <radius>
/stage location populate <locationId> <mobSlotId> <mobName> <count>
/stage location setboss <locationId> <mobSlotId>
/stage location clearboss <locationId> <mobSlotId>
/stage location clear <locationId> <mobSlotId>
/stage location remove <locationId> <mobSlotId>
/stage location here
/stage location list [locationId]
```

`location clear` удаляет population/spawn slots, но сохраняет геометрию зоны. `location remove` удаляет и population, и геометрию зоны. Runtime Boss Zone теперь определяется автоматически topology: боссом считается последний configured `mobSlotId` каждой Location, поэтому для progression вручную назначать boss не требуется. Если позже появляется более высокий configured slot, runtime boss автоматически переносится на него после reload configuration. `location setboss` и `clearboss` сохранены как legacy-команды для persisted `LocationKind`, но сохранённый kind больше не может сделать промежуточную stage runtime-боссом. Старый `/custommob location ...` сохранён как legacy alias для обратной совместимости, но новым каноническим путём является `/stage location ...`.

Точки перехода:

```text
/stage setpoint <locationId> <mobSlotId>
/stage setpoint <locationId> <mobSlotId> <x> <y> <z> [yaw pitch]
/stage clearpoint <locationId> <mobSlotId>
```

Короткий `setpoint` сохраняет текущую позицию и направление взгляда администратора. Точка может находиться в любом месте мира. Перед телепортом сервер повторно проверяет свободный bounding box и наличие твёрдой опоры.

Trigger-зона задаётся сразу для всех настроенных stage одной Location:

```text
/stage settrigger <locationId> <offset> <radius>
/stage cleartrigger <locationId>
```

Для mob-zone радиуса `R` внутренняя граница trigger-кольца равна `R + offset`, внешняя - `R + offset + radius`. Все stage внутри одной Location получают одинаковые `offset/radius`; разные Location могут использовать разные размеры.

Сообщения переходов также задаются сразу для всей Location:

```text
/stage setmessage <locked|forward|backward> <locationId> <text...>
/stage clearmessage <locked|forward|backward> <locationId>
/stage display <locationId> <mobSlotId> <chat|actionbar>
```

Для `/stage setmessage` и `/stage clearmessage` Tab completion предлагает `locked`, `forward`, `backward`.

Управление переходами и progression:

```text
/stage next [player]
/stage previous [player]
/stage teleport <locationId> <mobSlotId> [player]
/stage auto <on|off> [player]
/stage revoke <locationId> <mobSlotId> [player]
/stage reset <locationId> [player]
```

`next/previous` используют общий progression resolver и могут переходить через границу Location, если соседняя stage уже открыта и имеет безопасную transition point. После финальной runtime Boss Zone следующей stage является первый configured mobSlot следующей Location. `teleport` использует настроенную transition point выбранного stage и не открывает progression. `revoke` закрывает указанный stage и все последующие stage этой Location, а также очищает progression более поздних Location. `reset` сбрасывает указанную Location и все последующие.

`/stage auto <on|off>` относится к существующей автоматике trigger-зоны. Отдельный `Fast-auto` v1.3.11 является персональным persistent server-side флагом и переключается кнопкой в меню `L`; он не отключает и не заменяет trigger automation.

Игрок может открыть меню быстрого перехода клавишей `L`: сначала выбирается Location, затем уже открытый stage. Обычная stage отображается как `Моб N`, runtime Boss Zone - как `Босс`; подпись приходит из актуального server-side snapshot. Locked stage видим, но недоступен; stage без transition point не телепортирует. Финальное решение о телепорте всегда принимает сервер после повторной проверки progression и безопасности destination.

### Мобы

```text
/custommob create <name> <resource> <amount> <hp> <type>
/custommob setspawn <name>
/custommob unspawn <name>
/custommob delete <name>
/custommob list
```

Поддерживаемые типы мобов:

```text
zombie
skeleton
```

### Ресурсы

```text
/customresource create <name>
/customresource delete <resource>
/customresource list
/customresource balance <resource>
```

### Прежние колоды / наборы

```text
/customdeck create <name>
/customdeck switch <index>
/customdeck list
/customdeck delete <index>
```

Индексы начинаются с `0`.

### Руны / прежние карточки

```text
/customcards clear
```

Для пользовательских и административных команд, где это поддерживается, реализовано Tab-автодополнение с учётом уже введённого префикса.
