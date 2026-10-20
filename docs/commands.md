# Команды

Названия некоторых команд сохраняют прежнюю терминологию для обратной совместимости.

### Realm

```text
/realm join <realmId> [player]
```

Административная команда перехода в игровой Realm 1, 2 или 3. Realm 0 — прежний
административный overworld/dimension 0, в sync не участвует. Команды `/realm sync`
нет: Realm 2/3 полностью заменяются из Realm 1 только при старте выделенного
сервера до загрузки dimensions. [Контракт и проверки](realm-startup-sync.md).

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

Для пользовательских команд реализовано Tab-автодополнение с учётом уже введённого префикса.
