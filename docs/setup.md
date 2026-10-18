# Установка, настройка и сборка

## Требования

- Minecraft Java 1.12.2 и Forge 14.23.5.2859 на клиенте и сервере.
- Java 8; локальная сборка проекта использовала JDK 8u504.
- MongoDB: в окружении проекта используется 5.0.34, драйвер мода — 4.5.1.
- Для сборки используются Gradle Wrapper 7.6.4 и ForgeGradle 5.1.73.

## Установка готового мода

Скачайте JAR из [GitHub Releases](https://github.com/Theyyk/re-volution-mod/releases) и поместите в `mods` клиента и сервера. Удалите предыдущий JAR этого мода, чтобы не загрузить две версии одновременно. Версия на обеих сторонах должна совпадать. На клиенте включите один вариант [набора ресурсов v1.1.0](https://github.com/Theyyk/re-volution-resource-pack/releases/tag/v1.1.0), 64×64 или 128×128.

## MongoDB

Подключение настраивается параметрами JVM:

```text
-Drevolution.mongo.uri=mongodb://localhost:27017
-Drevolution.mongo.database=MyProject_test
```

Имя базы можно переопределить параметром JVM. В текущем локальном окружении проекта используется одна постоянная база `MyProject_test`; её данные сохраняются между запусками. Если подключение требует авторизации, задайте соответствующий URI при запуске; не публикуйте его в репозитории.

MongoDB хранит игроков, наборы и руны, собственных мобов и пользовательские ресурсы. Сервер управляет балансом, наградами, состоянием рун и расчётом урона.

## Запуск выделенного сервера

Из каталога подготовленного сервера с файлами Forge, библиотеками, конфигурацией и принятым соглашением EULA:

```powershell
java -Drevolution.mongo.uri=mongodb://localhost:27017 -Drevolution.mongo.database=MyProject_test -jar forge-1.12.2-14.23.5.2859.jar nogui
```

Команда `java` должна указывать на Java 8. Клиент запускается через установленный профиль Forge. Подключитесь к адресу своего сервера; клавиша `C` открывает интерфейс мода. Для штатной остановки сервера используйте `stop`.

Локальные вспомогательные сценарии разработки находятся вне этого репозитория и не являются частью публичной установки проекта.

Текущий локальный workflow использует один постоянный сервер: мир и MongoDB сохраняются между запусками и не заменяются снимками другого окружения.

Для обычной установки используйте JAR из [GitHub Releases](https://github.com/Theyyk/re-volution-mod/releases), установите ту же версию мода на клиент и сервер и запускайте Forge с параметрами MongoDB, указанными выше.

Связанный набор ресурсов:
[RE:volution Resource Pack](https://github.com/Theyyk/re-volution-resource-pack).

## Сборка из исходников

В `gradle.properties` указан локальный Windows-путь `org.gradle.java.home`. Для другого окружения переопределите его своим JDK 8:

```powershell
.\gradlew.bat '-Dorg.gradle.java.home=C:\Program Files\Java\jdk8u504-b01' clean build
```

В Linux или GitHub Actions:

```bash
./gradlew -Dorg.gradle.java.home="$JAVA_HOME" -Dnet.minecraftforge.gradle.check.certs=false clean build
```

`JAVA_HOME` должен указывать на JDK 8. Готовый JAR появляется в `build/libs/`. [Проверки и процесс выпуска](development.md).
