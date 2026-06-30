# RMPD — система заполнения деклараций для перевозчиков

Программный продукт для подготовки и отправки электронных уведомлений **RMPD100** в польскую систему **PUESC** (Platforma Usług Elektronicznych Skarbowo-Celnych) в рамках реестра **SENT** (System Elektronicznego Nadzoru Transportu).

## Назначение

RMPD (Rejestracja Międzynarodowych Przewozów Drogowych) — обязательная регистрация международных автомобильных перевозок и каботажа на территории Польши для иностранных перевозчиков (ст. 28b закона о дорожном транспорте).

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

На этапе проектирования. Исходные спецификации XSD необходимо скачать с портала PUESC (архив `RMPD_v20.11.2024`) и разместить в `specs/rmpd/`.
