# RMPD — система заполнения деклараций для перевозчиков

Программный продукт для подготовки и отправки электронных уведомлений **RMPD100** в польскую систему **PUESC** (Platforma Usług Elektronicznych Skarbowo-Celnych) в рамках реестра **SENT** (System Elektronicznego Nadzoru Transportu).

## Назначение

RMPD (Rejestracja Międzynarodowych Przewozów Drogowych) — обязательная регистрация международных автомобильных перевозок и каботажа на территории Польши для иностранных перевозчиков (ст. 28b закона о дорожном транспорте).

## Быстрый старт (фаза 0)

### Требования

- Java 21, Maven 3.9+
- Node.js 22+, npm
- Docker Desktop (для compose)

### Спецификации PUESC

Скачайте XSD и WSDL по инструкции в [specs/README.md](specs/README.md) и распакуйте в `specs/rmpd/` и `specs/seap/`.

### Локальная разработка

```powershell
# Backend (порт 8080)
mvn -f backend/pom.xml spring-boot:run -pl rmpd-app -am

# Frontend (порт 4200, прокси /api → backend)
cd frontend
npm start
```

- UI: http://localhost:4200
- API health: http://localhost:8080/api/v1/health
- Swagger UI: http://localhost:8080/swagger-ui/index.html

### Docker Compose

```powershell
cd deploy
copy .env.example .env
docker compose up --build
```

- UI: http://localhost:4200
- Backend: http://localhost:8080
- MySQL (host): порт **3307** (внутри compose — 3306)

## Структура репозитория

```
rmpd/
├── frontend/          # Angular 19 + Material
├── backend/           # Spring Boot 3 multi-module
├── deploy/            # Docker Compose, Dockerfiles
├── specs/             # XSD/WSDL (вручную с PUESC)
└── docs/              # Документация
```

## Документация

| Документ | Описание |
|----------|----------|
| [docs/product-specification.md](docs/product-specification.md) | Спецификация организации продукта (стек, модули, API, этапы) |
| [docs/implementation-plan.md](docs/implementation-plan.md) | План реализации по спринтам и критериям приёмки |
| [docs/puesc-api.md](docs/puesc-api.md) | Интеграция с API PUESC через канал SEAP (SOAP/WebService) |
| [docs/rmpd100.md](docs/rmpd100.md) | Бизнес-логика и структура формы RMPD100 |
| [docs/architecture.md](docs/architecture.md) | Архитектура компонентов и потоков данных |

## Официальные ресурсы PUESC

- Продакшн: https://puesc.gov.pl/
- Тестовая среда: https://test.puesc.gov.pl/
- Спецификации SENT/RMPD: https://puesc.gov.pl/uslugi/uslugi-sieciowe-informacje-i-specyfikacje/system-sent
- Каналы связи SEAP: https://puesc.gov.pl/uslugi/uslugi-sieciowe-informacje-i-specyfikacje/kanaly-komunikacyjne
- FAQ по RMPD: https://puesc.gov.pl/faq/-/categories/714841982

## Статус проекта

**Фаза 0:** scaffold монорепозитория, Docker Compose, CI, OpenAPI skeleton.

**Фаза 1 / Спринт 1:** JWT auth, multi-tenant CRUD справочников, профиль перевозчика.

Demo-вхід (після старту backend): `admin@demo.local` / `admin123`

> При обновлении с фазы 0 сбросьте volume MySQL в Docker (`docker compose down -v`), т.к. таблица `user` переименована в `rmpd_user`.

Следующий шаг — спринт 2: CRUD деклараций, мастер RMPD100 (шаги 1–2).
