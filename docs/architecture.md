# Архитектура продукта RMPD

Рекомендуемая архитектура системы заполнения и отправки RMPD100.

---

## 1. Компоненты

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (Angular + Material)            │
│  ┌──────────┐ ┌───────────┐ ┌───────────┐ ┌────────────────────┐│
│  │ Форма    │ │ Список    │ │ Детали    │ │ Статус отправки    ││
│  │ RMPD100  │ │ деклараций│ │ декларации│ │ (polling/progress) ││
│  └──────────┘ └───────────┘ └───────────┘ └────────────────────┘│
└────────────────────────────┬────────────────────────────────────┘
                             │ REST API
┌────────────────────────────▼────────────────────────────────────┐
│                        Backend API                              │
│  ┌─────────────┐ ┌──────────────┐ ┌─────────────────────────┐   │
│  │ Declaration │ │ XML Generator│ │ PUESC Client (SEAP SOAP)│   │
│  │ Service     │ │ + XSD Valid. │ │ AcceptDocument / Get*   │   │
│  └─────────────┘ └──────────────┘ └─────────────────────────┘   │
│  ┌─────────────┐ ┌──────────────┐ ┌─────────────────────────┐   │
│  │ Dictionary  │ │ Signature    │ │ Job Queue (polling)     │   │
│  │ Sync        │ │ Service      │ │                         │   │
│  └─────────────┘ └──────────────┘ └─────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
         ┌─────────┐   ┌──────────┐   ┌──────────┐
         │   DB    │   │ PUESC    │   │ Словари  │
         │         │   │ SEAP API │   │ PUESC    │
         └─────────┘   └──────────┘   └──────────┘
```

---

## 2. Модули backend

### 2.1. Declaration Module

- CRUD деклараций RMPD100
- Хранение черновиков и отправленных
- Привязка к компании-перевозчику и пользователю
- История статусов

### 2.2. XML Generator

- Маппинг DTO → XML по XSD RMPD_v20.11.2024
- Валидация через libxml/xsd перед подписью
- Версионирование схем (при обновлении PUESC)

### 2.3. PUESC Client (SEAP)

- SOAP-клиент по WS_PULL.wsdl
- WS-Security PasswordDigest
- AcceptDocument, GetDocuments, GetNextDocument
- Retry с exponential backoff
- Логирование sysRef / korelacjaSysref

### 2.4. Signature Service

- XAdES-BES enveloped подпись
- Интеграция с HSM / файловым сертификатом
- Опционально: делегирование подписи пользователю (browser plugin)

### 2.5. Dictionary Sync

- Периодическая загрузка словарей с https://puesc.gov.pl/uslugi/slowniki
- Кэш: коды стран, типы ID, виды разрешений
- Валидация полей формы по актуальным кодам

### 2.6. Job Queue

- Асинхронный polling ответов PUESC после AcceptDocument
- Обработка UPP → UPO → бизнес-ответ
- Уведомления пользователю (email/push) при получении номера

---

## 3. Модель данных (основные сущности)

```
Carrier          — перевозчик (ID, тип, номер, название, адрес)
Vehicle          — ТС (страна, номера, GPS ID)
Permit           — разрешение (тип, номер, срок)
Route            — маршрут (даты, страны, точки PL)
Party            — отправитель/получатель
Declaration      — декларация RMPD100 (статус, sysRef, refNumber)
DeclarationEvent — событие (отправка, UPP, UPO, ошибка)
PuescCredential  — учётные данные PUESC (зашифровано)
```

### Статусы декларации

| Статус | Описание |
|--------|----------|
| `draft` | Черновик |
| `validated` | XSD-валидация пройдена |
| `signed` | Подписана |
| `submitted` | AcceptDocument выполнен, есть sysRef |
| `accepted` | UPP/UPO получены |
| `registered` | Получен номер референсный |
| `rejected` | NPP или бизнес-отказ |
| `error` | Техническая ошибка |

---

## 4. Frontend (Angular Material)

### Экраны

1. **Список деклараций** — mat-table с фильтрами, статусами
2. **Мастер RMPD100** — stepper (перевозчик → ТС → разрешение → маршрут → груз → подтверждение)
3. **Предпросмотр XML** — readonly + кнопка скачать
4. **Отправка** — прогресс, sysRef, номер референсный
5. **Настройки PUESC** — credentials, тест/прод, сертификат

### UX-правила

- Автосохранение черновика
- Условные поля (груз/порожний, EU/non-EU)
- Подсказки форматов (GPS: ZXX-XXXXXX-X)
- Валидация латиницы в реальном времени
- Прогресс заполнения (%)

---

## 5. Безопасность

| Аспект | Решение |
|--------|---------|
| Credentials PUESC | Шифрование at rest (AES-256), не логировать пароли |
| Сертификаты подписи | HSM или OS keychain, не хранить в БД |
| SOAP-трафик | HTTPS only |
| Аудит | Лог всех операций с PUESC (sysRef, timestamps) |
| Multi-tenant | Изоляция данных по carrier_id |

---

## 6. Этапы разработки

### Фаза 1 — MVP
- Форма RMPD100 (UI)
- Генерация XML + XSD-валидация
- Ручная загрузка XML на портал (без API)

### Фаза 2 — API интеграция
- SEAP SOAP клиент (test environment)
- AcceptDocument + polling
- Хранение sysRef и статусов

### Фаза 3 — Production
- XAdES подпись
- Синхронизация словарей
- RMPD (актуализация) и RMPD406
- Мониторинг и алерты

### Фаза 4 — Расширения
- OCR CMR → автозаполнение
- Мультиперевозчик / агентская модель (idSiscROP)
- Интеграция с GPS-провайдерами

---

## 7. Технологический стек

| Слой | Технология |
|------|------------|
| Frontend | Angular 19+, Angular Material |
| Backend | Java 21, Spring Boot 3.x |
| SOAP | JAX-WS / Spring WS client из WS_PULL.wsdl |
| XML/XSD | JAXB + javax.xml.validation (XSD) |
| Подпись | EU DSS (Digital Signature Service) / XAdES-BES |
| DB | MySQL 8.x |
| Queue | Spring `@Scheduled` / Spring Batch |
| OCR CMR | Tesseract / cloud OCR (см. product-specification.md) |
| Deploy | Docker, env: TEST / PROD PUESC |

> Полная спецификация организации продукта: [product-specification.md](product-specification.md)

---

## 8. Конфигурация окружений

```env
# .env.example
PUESC_ENV=test                          # test | prod
PUESC_WSDL_URL=https://te-ws.puesc.gov.pl/seap_wsChannel/DocumentHandlingPort?wsdl
PUESC_USERNAME=user@example.com
PUESC_PASSWORD=***                      # или из secrets manager
PUESC_TARGET_SYSTEM=SENT
RMPD_XSD_PATH=./specs/rmpd/RMPD_v20.11.2024
SIGNATURE_CERT_PATH=./certs/signing.p12
SIGNATURE_CERT_PASSWORD=***
```

---

## 9. Мониторинг

- Метрики: успешность AcceptDocument, время до refNumber, ошибки XSD
- Алерт при B010 (аварийный режим SEAP)
- Healthcheck: доступность WSDL endpoint
- Dashboard: декларации по статусам за период
