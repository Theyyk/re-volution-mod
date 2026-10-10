# Команды

Названия некоторых команд сохраняют прежнюю терминологию для обратной совместимости.

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
