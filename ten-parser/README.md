# ten-parser

Парсер тендерных площадок. На текущий момент поддерживается **Сбербанк-АСТ**.

## 📋 Возможности

- Поиск закупок по ключевым словам (одно или несколько).
- Фильтрация по статусу закупки (один или несколько).
- Выгрузка нескольких страниц результатов.
- Случайная задержка между запросами (имитация человеческого поведения).
- Настраиваемые таймауты подключения и чтения.
- Вывод в консоль: номер и наименование закупки.
- Запуск локально или в Docker.

## 🛠️ Технологии

| Компонент | Версия |
|-----------|--------|
| Java | 21 (LTS) |
| Spring Boot | 3.5.5 |
| Gradle | 9.x (wrapper) |
| picocli | 4.7.6 |
| Jackson | 2.18.2 |
| SLF4J + Logback | — |
| Docker | 24+ |


## 🚀 Запуск

### Требования

- JDK 21+
- Docker (опционально)

### Локально

```bash
# Сборка
./gradlew clean build

# Запуск (Linux/macOS)
./gradlew bootRun --args="--keyword kaspersky --status 'В процессе проведения' --pages 2"

# Запуск (Windows PowerShell)
./gradlew bootRun --args="--keyword kaspersky --status ""В процессе проведения"" --pages 2"
```

**Альтернатива (в PowerShell):**

```powershell
./gradlew bootJar
java -jar build/libs/ten-parser-1.0.0.jar --keyword kaspersky --status 'В процессе проведения' --pages 2
```

### Docker

```bash
# Сборка образа
docker compose build

# Запуск с аргументами
docker compose run --rm ten-parser --keyword kaspersky --status "В процессе проведения" --pages 2

# Несколько ключевых слов
docker compose run --rm ten-parser --keyword kaspersky usb --status "В процессе проведения" --pages 2

# Несколько статусов
docker compose run --rm ten-parser --keyword kaspersky --status "В процессе проведения,Подача заявок" --pages 2
```

## 🎛️ Параметры CLI

| Параметр | Обязателен | По умолчанию | Описание |
|----------|------------|--------------|----------|
| `--keyword` | ✅ Да | — | Ключевые слова для поиска (можно несколько через пробел) |
| `--status` | ❌ Нет | `В процессе проведения` | Статусы закупок через запятую |
| `--pages` | ❌ Нет | `2` | Сколько страниц выгрузить |
| `--help` | ❌ Нет | — | Показать справку |
| `--version` | ❌ Нет | — | Показать версию |

### Примеры

```bash
# Одно ключевое слово, один статус
--keyword kaspersky --status "В процессе проведения" --pages 2

# Несколько ключевых слов (anyWord)
--keyword kaspersky usb "флешка" --status "В процессе проведения" --pages 2

# Несколько статусов
--keyword kaspersky --status "В процессе проведения,Подача заявок,Прием заявок" --pages 2

# Справка
--help
```

## ⚙️ Настройки

Все настройки — в `src/main/resources/application.yml`.

| Параметр | По умолчанию | Описание |
|----------|--------------|----------|
| `sberbank.base-url` | `https://www.sberbank-ast.ru` | Базовый URL площадки |
| `sberbank.api-path` | `/api/Processing/main` | Путь к API поиска |
| `sberbank.page-size` | `20` | Размер страницы |
| `sberbank.start-public-date` | `01.01.2024` | Начало периода публикации |
| `sberbank.end-public-date` | `09.10.2026 23:59:59` | Конец периода публикации |
| `sberbank.connect-timeout-seconds` | `30` | Таймаут подключения |
| `sberbank.read-timeout-seconds` | `60` | Таймаут чтения ответа |
| `sberbank.delay-min-millis` | `1500` | Минимальная задержка между запросами |
| `sberbank.delay-max-millis` | `3000` | Максимальная задержка между запросами |

### Логирование

Уровень `DEBUG` для `ru.ten.parser` включён в `logback-spring.xml`.
Чтобы отключить — измените `<logger name="ru.ten.parser" level="DEBUG"/>` на `INFO`.

## 🧪 Тесты

```bash
# Все тесты
./gradlew test

# Только конкретный тест
./gradlew test --tests "ru.ten.parser.util.FilterBodyBuilderTest"

# С отчётом
./gradlew test jacocoTestReport
```

**Покрытие:**

| Класс | Тесты |
|-------|-------|
| `FilterBodyBuilder` | 5 тестов (сборка XML, экранирование) |
| `SearchResponse` | 3 теста (парсинг JSON, игнорирование полей, пустой список) |
| `TenderService` | 5 тестов (пагинация, статусы, задержки) |
| `RequestDelay` | 4 теста (границы, исключения) |

## 🏗️ Архитектура

```
CLI (picocli)
   ↓
ParserCommand → TenderService
                    ↓
              SberbankAstClient
                    ↓
              HttpClient (Java 21)
                    ↓
              Сбербанк-АСТ API
```

### Слои

- **cli** — парсинг аргументов, запуск команды.
- **service** — бизнес-логика: цикл по статусам, пагинация, задержки.
- **client** — HTTP-запросы к API площадки.
- **model** — DTO (records) для запроса и ответа.
- **util** — утилиты: сборка XML, задержка.
- **config** — Spring-бины: `HttpClient`, `ObjectMapper`, `SearchProperties`.

## 🔧 Разработка

### Добавление новой площадки

1. Создайте `NewPlatformClient` в `client/`.
2. Создайте `NewPlatformProperties` в `config/`.
3. Добавьте метод в `TenderService` или создайте отдельный сервис.
4. Добавьте опцию `--platform` в `ParserCommand`.

### Обновление User-Agent

Строка `User-Agent` в `application.yml` содержит версию Chrome.
При выходе новой версии — обновите:

```yaml
sberbank:
  user-agent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36"
```

### Отладка

Уровень `DEBUG` показывает:
- Тело POST-запроса.
- Статус и размер ответа.
- Задержки между запросами.

## 🐳 Docker

### Сборка

```bash
docker compose build
```

### Запуск

```bash
docker compose run --rm ten-parser --keyword kaspersky --pages 2
```

### Размер образа

- **~490 МБ** — стандартный JRE + fat jar.
- Можно уменьшить, заменив базовый образ на `eclipse-temurin:21-jre-alpine` (~150 МБ экономии).

### Очистка

```bash
# Удалить неиспользуемые образы
docker image prune -a

# Удалить build cache старше 7 дней
docker builder prune --filter until=168h
```

## 📝 Лицензия

Внутренний проект. Не для распространения.

## 👥 Авторы

- **ten-parser team** — разработка и поддержка.

## 📚 Полезные ссылки

- [picocli documentation](https://picocli.info/)
- [Spring Boot documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Java 21 HttpClient](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html)
- [Jackson documentation](https://github.com/FasterXML/jackson-docs)