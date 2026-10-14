# ten-parser

Парсер тендерных площадок **Сбербанк-АСТ** и **РТС-Тендер**.

- [Часть 1. Быстрый способ получить данные со Сбербанк-АСТ через curl](#часть-1-быстрый-способ-получить-данные-со-сбербанк-аст-через-curl)
- [Часть 2. Аналогичный подход через curl не работает для РТС-Тендер](#часть-2-аналогичный-подход-через-curl-не-работает-для-ртс-тендер)
- [Часть 3. Реализованное приложение](#часть-3-реализованное-приложение)
- [Часть 4. Команды запуска (Docker)](#часть-4-команды-запуска-docker)
- [Часть 5. Команды запуска (локально)](#часть-5-команды-запуска-локально)
- [Часть 6. Параметры CLI](#часть-6-параметры-cli)
- [Часть 7. Настройки](#часть-7-настройки)
- [Часть 8. Архитектура](#часть-8-архитектура)

---

## Часть 1. Быстрый способ получить данные со Сбербанк-АСТ через curl

Сбербанк-АСТ предоставляет **stateless API** без куки и токенов. Достаточно одного POST-запроса с XML внутри JSON. Куки не нужны — проверено экспериментально.

### 1.1. Создание файла тела запроса

**PowerShell (однострочная для копирования):**

```powershell
Set-Content -Path "sber_filter.json" -Value '{"windowCode":"/EsOpenUnitedPurchaseList","actionType":"MONITOR","actionCode":"default","documentBody":"","parm":"es","filterBody":"<query><targetPageCode>OTUnitedPurchaseList</targetPageCode><searchbyorg>1</searchbyorg><pagesize>20</pagesize><pagenum>0</pagenum><sortOrder>desc</sortOrder><searchBarType>oneWord</searchBarType><mainInput>kaspersky</mainInput><publicDate><startPublicDate>01.01.2024</startPublicDate><endPublicDate>09.10.2026 23:59:59</endPublicDate></publicDate><purchStateNameTerm>В процессе проведения</purchStateNameTerm><fields><field>hasSt14</field></fields><sortType></sortType><requestEndDate><startRequestDate></startRequestDate><endRequestDate></endRequestDate></requestEndDate><IsSMPTerm></IsSMPTerm></query>","options":{}}' -Encoding UTF8
```

**Bash:**

```bash
cat > sber_filter.json <<'EOF'
{"windowCode":"/EsOpenUnitedPurchaseList","actionType":"MONITOR","actionCode":"default","documentBody":"","parm":"es","filterBody":"<query><targetPageCode>OTUnitedPurchaseList</targetPageCode><searchbyorg>1</searchbyorg><pagesize>20</pagesize><pagenum>0</pagenum><sortOrder>desc</sortOrder><searchBarType>oneWord</searchBarType><mainInput>kaspersky</mainInput><publicDate><startPublicDate>01.01.2024</startPublicDate><endPublicDate>09.10.2026 23:59:59</endPublicDate></publicDate><purchStateNameTerm>В процессе проведения</purchStateNameTerm><fields><field>hasSt14</field></fields><sortType></sortType><requestEndDate><startRequestDate></startRequestDate><endRequestDate></endRequestDate></requestEndDate><IsSMPTerm></IsSMPTerm></query>","options":{}}
EOF
```

### 1.2. Запрос + вывод номера и наименования

**PowerShell (однострочная для копирования):**

```powershell
curl.exe -s -X POST "https://www.sberbank-ast.ru/api/Processing/main" -H "Content-Type: application/json" -H "Accept: application/json" -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36" -H "Origin: https://www.sberbank-ast.ru" -H "Referer: https://www.sberbank-ast.ru/UnitedPurchaseList.html" -H "x-ast-actioncode: default" -H "x-ast-actiontype: MONITOR" -H "x-ast-pageurl: https://www.sberbank-ast.ru/UnitedPurchaseList.html" -H "x-ast-windowcode: /EsOpenUnitedPurchaseList" -d "@sber_filter.json" -o "response.json"; $json = [System.IO.File]::ReadAllText("$PWD\response.json", [System.Text.Encoding]::UTF8); ($json | ConvertFrom-Json).PurchaseList | ForEach-Object { "Номер: $($_.'_source'.purchCode)`nНаименование: $($_.'_source'.purchName)`n---" }
```

**Bash:**

```bash
curl -s -X POST "https://www.sberbank-ast.ru/api/Processing/main" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36" \
    -H "Origin: https://www.sberbank-ast.ru" \
    -H "Referer: https://www.sberbank-ast.ru/UnitedPurchaseList.html" \
    -H "x-ast-actioncode: default" \
    -H "x-ast-actiontype: MONITOR" \
    -H "x-ast-pageurl: https://www.sberbank-ast.ru/UnitedPurchaseList.html" \
    -H "x-ast-windowcode: /EsOpenUnitedPurchaseList" \
    -d "@sber_filter.json" \
| jq -r '.PurchaseList[] | "Номер: \(._source.purchCode)\nНаименование: \(._source.purchName)\n---"'
```

### 1.3. Требования

- **PowerShell** + **`curl.exe`** — в Windows.
- **Кодировка UTF-8** — в PowerShell требуется `[System.IO.File]::ReadAllText(..., UTF8)`, иначе кириллица превратится в «кракозябры».
- **`jq`** — только для Bash (для установки: `apt install jq` / `brew install jq`).

---

## Часть 2. Аналогичный подход через curl не работает для РТС-Тендер

### 2.1. Чтобы получить данные с РТС-Тендер требуется:

- сначала выполнить запрос **GET** на страницу поиска, чтобы получить cookies (`XSRF-TOKEN`, `.AspNetCore.Antiforgery`,... tcm и другие) и токен `fotoken`;
- извлечь токен `fotoken` и `XSRF-TOKEN` из HTML-кода или заголовка ответа от первого запроса —
- поиск через **POST** на `/poisk/search/ajaxwithfullmodel` с JSON-телом.
  В запросе есть (согласно HAR-файлу):
  - **Cookies:** `XSRF-TOKEN`, `.AspNetCore.Antiforgery`, `PublicSite_FrontOffice`, `tcm`, `.ASPXANONYMOUS` и ещё ~30 штук.
  - **Заголовки:** `X-XSRF-TOKEN`, `fotoken`, `X-Requested-With: XMLHttpRequest`.
  - **Ответ:** HTML (не JSON).
- распарсить полученный HTML.

### 2.2. Anti-DDoS защита

После **GET**-запроса получаемый через curl `файл.html` — не страница поиска, 
а JavaScript-заглушка Anti-DDoS защиты. Это лишает возможности получить токены.

### 2.3. Первая попытка: гибридный подход (Selenium + HttpClient)

**Идея:** Selenium проходит Anti-DDoS, забирает cookies и `fotoken`, а дальше `HttpClient` быстро отправляет POST-запросы.

**Что получилось:**
- Selenium успешно проходил Anti-DDoS.
- Cookies забирались, `XSRF-TOKEN` был получен.
- **Но `fotoken` = `null`.**

**Результат:** POST-запрос возвращал **HTTP 503** с заглушкой Anti-DDoS.

### 2.4. Почему `fotoken` не удалось получить

Из HAR видно, что `fotoken` приходит через **WebSocket** на `wss://www.rts-tender.ru/poisk/verification`:

```
→ {"arguments":[],"invocationId":"0","target":"SetToken","type":1}
← {"type":1,"target":"GetToken","arguments":["CfDJ8CIepwwZJ2RHhqSAMtgtPRsG_wyO3KtJPjneLKrsvApaQP7T59MW3bKrHe8_J8e8G0rs5-TR38xboFbUV_hrG_9wXnKHvdiKhelMv6xUKxH-WwG3D5f0GO2dQAv6LOG1oRhwhnjHgg0l7vdoK3ELR43mxQJtaETgdy4l7fx8r5TX"]}
```

Это **SignalR-хаб**. Токен `fotoken` живёт **в памяти JavaScript**, 
не сохраняется ни в cookies, ни в `localStorage`, ни в `sessionStorage`. 
Перехватить его без CDP (Chrome DevTools Protocol) невозможно.

**Кроме того**:  
Anti-DDoS защита площадки вставляет JavaScript-челлендж, который выполняется 5 секунд — это барьер против автоматизации, а не случайная медлительность.
Гибридная схема требует дополнительных проверок (в отличие от чистого Selenium, где браузер сам управляет cookies, токенами и заголовками):
- следить за актуальностью `fotoken` (эфемерный токен в памяти JS, исчезает при закрытии браузера, без него POST возвращает 503);
- проверять совпадение `XSRF-TOKEN` с заголовком `X-XSRF-TOKEN` (при несовпадении — 403);
- учитывать срок жизни сессионных cookies (`PublicSite_FrontOffice`, `.AspNetCore.Antiforgery`, `tcm`) — живут до закрытия браузера.  

Каждая ошибка (503/403) или истечение срока cookies требует retry с повторным прохождением Anti-DDoS.

### 2.5. Вывод

Чистый Selenium — самый простой и надёжный вариант. Он сам проходит Anti-DDoS, сам получает fotoken и сам отправляет POST с правильными заголовками. Мы просто читаем результат из DOM.
Гибридный Selenium + HttpClient будет подходящим, если требуется максимально ускорить получение данных из площадки РТС-Тендер при массовых регулярных сборах.

---

## Часть 3. Реализованное приложение

### 3.1. Что умеет

- **Сбербанк-АСТ:** поиск через `HttpClient` + JSON API. Куки не нужны. Параметр pagesize можно настраивать — от 20 до сотен записей на запрос.
- **РТС-Тендер:** поиск через Selenium + Jsoup. Проходит Anti-DDoS, работает с пагинацией и фильтром статуса.
- **Фильтр по статусу:** для Сбербанк-АСТ — через `<purchStateNameTerm>`, для РТС-Тендер — через модалку фильтров.
- **Пагинация:** `--pages N` — сколько страниц выгрузить.
- **Ключевые слова:** `--keyword kaspersky`.
- **Несколько статусов:** `--status "Активные,Архив"` (цикл по каждому).
- **Случайные задержки:** между запросами (1.5–3 сек для Сбербанк-АСТ, 2–4 сек для РТС-Тендер).
- **Логирование:** SLF4J + Logback, уровень DEBUG для `ru.ten.parser`.

### 3.2. Технологии

| Компонент | Версия |
|-----------|--------|
| Java | 21 (LTS) |
| Spring Boot | 3.5.5 |
| Gradle | 9.x |
| picocli | 4.7.6 |
| Jackson | 2.18.2 |
| Jsoup | 1.18.1 |
| Selenium | 4.31.0 |
| SLF4J + Logback | — |
| Docker | 24+ |

---

## Часть 4. Команды запуска (Docker)
Команды работают одинаково в Bash и PowerShell.

### 4.1. Сборка образа

```bash
docker compose build
```

### 4.2. Запуск для Сбербанк-АСТ

```powershell
docker compose run --rm ten-parser --platform sberbank --keyword kaspersky --status "В процессе проведения" --pages 2
```

### 4.3. Запуск для РТС-Тендер

```powershell
docker compose run --rm ten-parser --platform rts --keyword kaspersky --status "Активные" --pages 2
```

### 4.4. Несколько ключевых слов

```bash
docker compose run --rm ten-parser --platform sberbank --keyword kaspersky usb "флешка" --status "В процессе проведения" --pages 2
```

### 4.5. Несколько статусов

```bash
docker compose run --rm ten-parser --platform sberbank --keyword kaspersky --status "В процессе проведения,Подача заявок" --pages 2
```

### 4.6. Очистка

```bash
docker image prune -a
docker builder prune --filter until=168h
```

---

## Часть 5. Команды запуска (локально)

> **Примечание для PowerShell.** Чтобы в выводе не было «кракозябр», выполните перед запуском:
> ```powershell
> chcp 65001 | Out-Null
> [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
> ```

### 5.1. Сборка проекта

**Bash / PowerShell:**

```bash
./gradlew clean build
```

### 5.2. Запуск через `bootRun` (для отладки)

**Bash:**

```bash
./gradlew bootRun --args="--platform sberbank --keyword kaspersky --status 'В процессе проведения' --pages 2"
```

**PowerShell (однострочная):**

```powershell
./gradlew bootRun --args="--platform sberbank --keyword kaspersky --status 'В процессе проведения' --pages 2"
```

### 5.3. Запуск через собранный JAR

**Шаг 1. Сборка JAR**

**Bash / PowerShell:**

```bash
./gradlew bootJar
```

**Шаг 2. Запуск**

**Bash:**

```bash
java -jar build/libs/ten-parser-1.0.0.jar \
    --platform sberbank \
    --keyword kaspersky \
    --status "В процессе проведения" \
    --pages 2
```

**PowerShell (однострочная):**

```powershell
java -jar build/libs/ten-parser-1.0.0.jar --platform sberbank --keyword kaspersky --status "В процессе проведения" --pages 2
```

### 5.4. Запуск для РТС-Тендер

**Bash / PowerShell:**

```bash
java -jar build/libs/ten-parser-1.0.0.jar --platform rts --keyword kaspersky --status "Активные" --pages 2
```

### 5.5. Справка по параметрам

**Bash / PowerShell:**

```bash
java -jar build/libs/ten-parser-1.0.0.jar --help
```

---

## Часть 6. Параметры CLI

| Параметр | Обязателен | По умолчанию | Описание |
|----------|------------|--------------|----------|
| `--keyword` | ✅ Да | — | Ключевые слова (можно несколько через пробел) |
| `--status` | ❌ Нет | `В процессе проведения` | Статусы через запятую |
| `--pages` | ❌ Нет | `2` | Сколько страниц выгрузить |
| `--platform` | ❌ Нет | `sberbank` | Площадка: `sberbank`, `rts` |

### Статусы для Сбербанк-АСТ (активные и новые):

- `В процессе проведения` — основной активный статус
- `Многолотовая процедура` — закупка с несколькими лотами
- `Новый(-ая)`, `Опубликован(-а)` — самые ранние этапы
- `Прием заявок`, `Подача заявок`, `Рассмотрение заявок` — этапы рассмотрения

### Статусы для РТС-Тендер

- `Активные` (по умолчанию)
- `Рассмотрение первых частей заявок`
- `Идут торги`
- `Подписание контракта`
- `Архив`

**Важно:** статус `Определение победителя` **не поддерживается** — он виртуальный и управляется дочерними чекбоксами.

---

## Часть 7. Настройки

Все настройки — в `src/main/resources/application.yml`.

### Сбербанк-АСТ

| Параметр | По умолчанию | Описание |
|----------|--------------|----------|
| `sberbank.base-url` | `https://www.sberbank-ast.ru` | Базовый URL |
| `sberbank.api-path` | `/api/Processing/main` | Путь к API |
| `sberbank.page-size` | `20` | Размер страницы |
| `sberbank.start-public-date` | `01.01.2024` | Начало периода |
| `sberbank.end-public-date` | `09.10.2026 23:59:59` | Конец периода |
| `sberbank.delay-min-millis` | `1500` | Мин. задержка |
| `sberbank.delay-max-millis` | `3000` | Макс. задержка |

### РТС-Тендер

| Параметр | По умолчанию | Описание |
|----------|--------------|----------|
| `rts.base-url` | `https://www.rts-tender.ru` | Базовый URL |
| `rts.search-path` | `/poisk/search` | Путь к странице поиска |
| `rts.delay-min-millis` | `2000` | Мин. задержка |
| `rts.delay-max-millis` | `4000` | Макс. задержка |
| `rts.headless` | `true` | Headless-режим Chrome |

### Логирование

Уровень DEBUG для `ru.ten.parser` включён в `logback-spring.xml`. Чтобы отключить — измените на `INFO`.

---

## Часть 8. Архитектура

### Слои

- **cli** — парсинг аргументов (picocli), запуск команды.
- **service** — бизнес-логика: цикл по статусам, пагинация.
- **client** — HTTP/Selenium-клиенты площадок.
    - `TenderClient` — общий интерфейс.
    - `SberbankAstClient` — HttpClient + Jackson.
    - `RtsTenderClient` — Selenium + Jsoup.
- **model** — DTO (records) для запроса и ответа.
- **util** — утилиты: `FilterBodyBuilder`, `RequestDelay`.
- **config** — Spring-бины: `HttpClient`, `ObjectMapper`, `WebDriver`, properties.

### Схема

```
CLI (picocli)
   ↓
ParserCommand → TenderService
                    ↓
         ┌──────────┴──────────┐
         ↓                     ↓
SberbankAstClient       RtsTenderClient
 (HttpClient)            (Selenium)
         ↓                     ↓
    JSON API               HTML (сериализованный DOM)
         ↓                     ↓
      Jackson                Jsoup
         ↓                     ↓
    Purchase              Purchase
         └──────────┬──────────┘
                    ↓
              Консоль
         "Номер: ..."
         "Наименование: ..."
```

## Приложение А. Известные ограничения

- **РТС-Тендер работает только в headless-режиме.** Если Anti-DDoS начнёт детектировать headless — установите `rts.headless: false`.
- **Версия ChromeDriver должна совпадать с Chrome.** Selenium Manager (встроен в Selenium 4.6+) скачивает автоматически.
- **`fotoken` невозможно перехватить без CDP** — используется чистый Selenium вместо гибридного подхода.
- **Модалка фильтров РТС-Тендер** открывается кнопкой `.main-search__settings`. Если селектор изменится — обновите в `RtsTenderClient.applyStatusFilterAndSearch`.

---

## Приложение Б. Обновление User-Agent

Строка `User-Agent` в `application.yml` содержит версию Chrome. При выходе новой версии — обновите:

```yaml
sberbank:
  user-agent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36"

rts:
  user-agent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36"
```

---

## Приложение В. Известные ограничения Docker-сборки

Текущая реализация Docker-образа **не оптимизирована по времени старта**. 

При запуске контейнера приложение тратит **~60 секунд** до начала работы:

- **Spring создаёт `RtsTenderClient` и `WebDriver` при старте**, даже если выбран `--platform sberbank` (Chrome для Сбербанк-АСТ не нужен).
- **Selenium Manager скачивает ChromeDriver** при каждом запуске, потому что `docker compose run --rm` уничтожает кэш `/tmp/selenium-cache`.
- **Chrome стартует в headless-режиме** ~10–15 секунд.

### Что можно улучшить

| Решение | Экономия |
|---------|----------|
| `ObjectProvider` в `TenderService` для ленивой инициализации клиентов | ~55 сек для `sberbank` |
| Docker volume для кэша ChromeDriver | ~20–30 сек |
| Предустановка ChromeDriver в образ | ~20–30 сек |
| JVM-флаг `-XX:TieredStopAtLevel=1` | ~1–2 сек |


## Лицензия

Внутренний проект. Не для распространения.