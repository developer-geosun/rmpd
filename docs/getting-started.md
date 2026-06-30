# Запуск проекта RMPD

Инструкция по локальной разработке и запуску через Docker.

**Связанные документы:** [README.md](../README.md) · [architecture.md](architecture.md)

---

## 1. Требования

| Компонент | Версия |
|-----------|--------|
| Java | 21 |
| Maven | 3.9+ |
| Node.js | 22+ |
| npm | актуальный |
| MySQL | 8.0 (локально или в Docker) |
| Docker Desktop | опционально, для полного стека |

Проверка:

```powershell
java -version
mvn -version
node -version
npm -version
```

---

## 2. Первоначальная настройка

### 2.1. Клонирование и зависимости frontend

```powershell
cd e:\MyProjects\GeoSun\rmpd\frontend
npm install
```

### 2.2. Спецификации PUESC (опционально)

Для XSD-валидации XML скачайте архив `RMPD_v20.11.2024` и распакуйте по инструкции в [specs/README.md](../specs/README.md).

Без XSD проект запускается; валидация по схеме будет пропущена.

### 2.3. Переменные окружения (опционально)

Пример для Docker: скопируйте `deploy/.env.example` → `deploy/.env`.

Ключевые переменные:

| Переменная | По умолчанию | Описание |
|------------|--------------|----------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/rmpd?...` | URL MySQL |
| `SPRING_DATASOURCE_USERNAME` | `rmpd` | Пользователь БД |
| `SPRING_DATASOURCE_PASSWORD` | `rmpd` | Пароль БД |
| `RMPD_JWT_SECRET` | см. `application.yml` | Секрет JWT |
| `PUESC_CLIENT` | `mock` | `mock` или `http` (test.puesc) |
| `STORAGE_CMR_PATH` | `./data/cmr` | Хранение файлов CMR |
| `RMPD_MAIL_ENABLED` | `false` | Email при регистрации RMPD |

---

## 3. Вариант A — локальная разработка

Подходит для ежедневной работы: backend и frontend на хосте, MySQL в Docker или локально.

### Шаг 1. База данных

**Вариант A1 — только MySQL в Docker (рекомендуется):**

```powershell
cd e:\MyProjects\GeoSun\rmpd\deploy
copy .env.example .env
docker compose up mysql -d
```

MySQL доступен на хосте на порту **3307** (внутри контейнера — 3306).

Перед запуском backend задайте URL (PowerShell):

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3307/rmpd?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME="rmpd"
$env:SPRING_DATASOURCE_PASSWORD="rmpd"
```

**Вариант A2 — MySQL установлен локально на порту 3306:**

Создайте БД и пользователя:

```sql
CREATE DATABASE rmpd CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'rmpd'@'localhost' IDENTIFIED BY 'rmpd';
GRANT ALL PRIVILEGES ON rmpd.* TO 'rmpd'@'localhost';
FLUSH PRIVILEGES;
```

Дополнительные переменные не нужны — используются значения по умолчанию из `application.yml`.

### Шаг 2. Backend (терминал 1)

```powershell
cd e:\MyProjects\GeoSun\rmpd
mvn -f backend/pom.xml spring-boot:run -pl rmpd-app -am
```

При первом запуске Flyway применит миграции. Создаётся demo-пользователь (см. раздел 5).

### Шаг 3. Frontend (терминал 2)

```powershell
cd e:\MyProjects\GeoSun\rmpd\frontend
npm start
```

Прокси `/api` → `http://localhost:8080` настроен в `frontend/proxy.conf.json`.

### Адреса

| Сервис | URL |
|--------|-----|
| Веб-интерфейс | http://localhost:4200 |
| API health | http://localhost:8080/api/v1/health |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Actuator | http://localhost:8080/actuator/health |

---

## 4. Вариант B — Docker Compose (весь стек)

Поднимает MySQL, backend и frontend (nginx) одной командой.

```powershell
cd e:\MyProjects\GeoSun\rmpd\deploy
copy .env.example .env
docker compose up --build
```

Остановка:

```powershell
docker compose down
```

Полный сброс БД (после изменений Flyway):

```powershell
docker compose down -v
docker compose up --build
```

| Сервис | URL / порт |
|--------|------------|
| UI | http://localhost:4200 |
| Backend | http://localhost:8080 |
| MySQL (с хоста) | `localhost:3307` |

---

## 5. Вход в систему

После старта backend (профиль не `test`) создаётся demo-аккаунт:

| Поле | Значение |
|------|----------|
| Email | `admin@demo.local` |
| Пароль | `admin123` |

Роль: **ADMIN** (доступ ко всем разделам, включая настройки PUESC).

---

## 6. Быстрая проверка работоспособности

1. Откройте http://localhost:4200 и войдите.
2. **Довідники** → **ТЗ** — добавьте транспортное средство.
3. **Декларації** → **Нова декларація** — заполните поля, сохраните.
4. **Перевірити** → **Відправити в PUESC** — в режиме `mock` статус станет `REGISTERED`.
5. Загрузите файл CMR (txt/pdf) на карточке декларации → **Застосувати вибрані**.

---

## 7. Настройки PUESC

По умолчанию `PUESC_CLIENT=mock` — интеграция с test.puesc не требуется.

Для реального test.puesc.gov.pl:

1. В `deploy/.env` или переменных окружения: `PUESC_CLIENT=http`
2. В UI: меню **PUESC** → email и пароль от аккаунта PUESC → **Зберегти** → **Тест з'єднання**

---

## 8. Сборка и тесты

```powershell
# Backend
mvn -f backend/pom.xml clean verify

# Frontend
cd frontend
npm run build
```

---

## 9. Типичные проблемы

### Порт 3306 занят

В `docker-compose.yml` MySQL проброшен на **3307**. Используйте URL с портом 3307 (см. раздел 3).

### Ошибки Flyway / «Table already exists»

Сбросьте volume MySQL:

```powershell
cd deploy
docker compose down -v
docker compose up mysql -d
```

### Backend не подключается к MySQL

- Убедитесь, что контейнер `rmpd-mysql` запущен: `docker compose ps`
- Проверьте `SPRING_DATASOURCE_URL`, логин и пароль
- Дождитесь healthcheck MySQL перед стартом backend в Docker

### Frontend: ошибки при `npm install`

В проекте используется `frontend/.npmrc` с `legacy-peer-deps=true`. Запускайте `npm install` из папки `frontend`.

### IDE показывает ошибки Java, но `mvn verify` успешен

Корень репозитория должен быть открыт как workspace. Выполните **Java: Clean Java Language Server Workspace** и убедитесь, что выбран JDK 21.

---

## 10. Структура портов (кратко)

```
localhost:4200  →  Angular (dev) или nginx (Docker)
localhost:8080  →  Spring Boot API
localhost:3307  →  MySQL (Docker Compose, с хоста)
```
