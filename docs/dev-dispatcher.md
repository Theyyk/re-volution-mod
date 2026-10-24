# Маршрутизация сборок DEV

DEV Docker dispatcher принимает только успешный `push` в **`develop`** из
`Theyyk/re-volution-mod`. Ветки feature/fix проходят собственный CI, но не
публикуют DEV до обычного merge в `develop`.

Допуск задан одновременно в `workflow_run.branches` и в условии job.
Stable tag (`v1.3.9`, будущие `v*`), main, release и посторонние ветки
не отправляют `mod-dev-build`. Наличие единственного актуального gameplay
artifact по-прежнему проверяется перед отправкой; документационный CI без JAR
не запускает Docker/client chain.

Проверка: `node .github/ci/test_dev_dispatcher.cjs`. Она вычисляет фактическое
условие job для успешного develop push, stable tags, ошибочных/отменённых
сборок, PR, ручного запуска и fork. Поля исторического tag run `40114068931`
воспроизводят дефект v1.3.9: на старом условии тест падает.

Stable dispatcher (`dispatch-stable-docker.yml`) не изменён: опубликованный
релиз и совпадение source SHA с commit тега проверяются его собственной цепочкой.
Эта правка не запускает PROD и не выполняет перенос world/Mongo из DEV в PROD.
