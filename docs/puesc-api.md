# Интеграция с API PUESC (канал SEAP)

Документ описывает техническую интеграцию системы с **PUESC** через **SEAP** (Single Electronic Access Point) — единый электронный шлюз KAS (Krajowa Administracja Skarbowa) для обмена XML-сообщениями.

> **Источник:** [SEAP PLUS Specyfikacja Techniczna Publiczna](https://puesc.gov.pl/uslugi/uslugi-sieciowe-informacje-i-specyfikacje/kanaly-komunikacyjne) v5.50, [Usługi sieciowe](https://puesc.gov.pl/uslugi/uslugi-sieciowe-informacje-i-specyfikacje)

---

## 1. Обзор архитектуры

```
┌─────────────────┐     SOAP 1.1 + WS-Security      ┌──────────────────┐
│  Наш продукт    │ ──────────────────────────────► │  SEAP (PUESC)    │
│  (RMPD system)  │ ◄────────────────────────────── │  DocumentHandling│
└─────────────────┘     AcceptDocument / Get*       └────────┬─────────┘
                                                             │
                                                             ▼
                                                    ┌──────────────────┐
                                                    │  SENT (RMPD)     │
                                                    │  система доменная│
                                                    └──────────────────┘
```

PUESC — это не REST API. Обмен идёт через **SOAP 1.1 WebService** с XML-документами, валидируемыми по **XSD**.

### Два уровня сообщений

| Уровень | Описание |
|---------|----------|
| **Транспорт (SEAP)** | Обёртка SOAP: `AcceptDocument`, `GetNextDocument` и т.д. |
| **Бизнес (SENT/RMPD)** | XML-документ RMPD100 внутри `document/content` (Base64) |

---

## 2. Среды и endpoints

| Среда | Портал | WSDL (WS_PULL) |
|-------|--------|----------------|
| **Продакшн** | https://puesc.gov.pl/ | `https://ws.puesc.gov.pl/seap_wsChannel/DocumentHandlingPort?wsdl` |
| **Тест** | https://test.puesc.gov.pl/ | `https://te-ws.puesc.gov.pl/seap_wsChannel/DocumentHandlingPort?wsdl` |

Дополнительные материалы:
- WSDL и XSD канала: архив «Załączniki do Specyfikacji» на [странице каналов](https://puesc.gov.pl/uslugi/uslugi-sieciowe-informacje-i-specyfikacje/kanaly-komunikacyjne)
- Адресация: файл `Adresacja PUESC.xlsx` (там же)
- Тестовая среда SISC: ссылка на [странице usług sieciowych](https://puesc.gov.pl/uslugi/uslugi-sieciowe-informacje-i-specyfikacje)

---

## 3. Предварительные требования

### 3.1. Аккаунт PUESC

1. Зарегистрировать аккаунт на https://puesc.gov.pl/ (или test.puesc.gov.pl)
2. Подтвердить личность
3. Зарегистрировать компанию-перевозчика
4. Получить право электронной подписи документов (профиль zaufany, kwalifikowany certyfikat или niekwalifikowany certyfikat celny)
5. Для API: логин WebService = **email аккаунта PUESC**, пароль = пароль от портала

### 3.2. Уровень доступа

- Для RMPD достаточно **базового** (PODSTAWOWY) уровня прав
- Для работы от имени компании — привязка представителя к фирме

### 3.3. Электронная подпись XML

Документ RMPD100 должен быть подписан электронной подписью (XAdES-BES, enveloped), если это требует XSD-спецификация SENT. Подпись может быть:
- встроена в XML до отправки через API, или
- добавлена на портале PUESC после загрузки файла

---

## 4. Аутентификация (WS-Security)

### Протокол

- **SOAP 1.1**
- **WS-Addressing** — обязателен заголовок `MessageID`
- **WS-Security UsernameToken** — режим `PasswordDigest` (расширение `PasswordDigestExt`)

### Алгоритм Password Digest

```
password_hash = Base64( SHA-1( password ) )          // UTF-8, в бинарном виде SHA-1
Password_Digest = Base64( SHA-1( nonce + created + password_hash ) )
```

Где:
- `nonce` — случайные байты, Base64
- `created` — timestamp UTC, ISO 8601, например `2021-09-24T14:41:50Z`
- `password` — пароль от аккаунта PUESC

### Ограничения по времени

- `created` не должен отличаться от текущего UTC более чем на **5 минут**
- Время жизни сообщения — **5 минут**; после истечения: `A security error was encountered when verifying the message`

### Типичные ошибки авторизации

| Причина | Симптом |
|---------|---------|
| Нет/неактивный аккаунт | SecurityError |
| Неверный Password Digest | SecurityError |
| Некорректный `created` (не UTC, >5 мин) | SecurityError |

---

## 5. Операции WS_PULL (основной канал для интегратора)

Интерфейс: `WS_PULL.wsdl`, синхронный режим.

### 5.1. AcceptDocument — отправка RMPD100

**Назначение:** передать XML-документ RMPD100 в PUESC.

**Запрос:** `AcceptDocumentRequest` с объектом `document`:

```xml
<!-- Концептуальная структура (см. WS_CHANNEL.xsd) -->
<document>
  <content filename="rmpd100.xml" mime="application/xml">
    <!-- Base64-кодированный XML RMPD100 (возможно с XAdES-подписью) -->
  </content>
  <attachments>  <!-- опционально, max 1 вложение -->
    <content filename="..." mime="application/pdf">...</content>
  </attachments>
  <targetSystems>
    <system>SENT</system>  <!-- идентификатор доменной системы -->
  </targetSystems>
</document>
```

**Ответ:** `AcceptDocumentResponse`:

```xml
<result>
  <sysRef>уникальный-идентификатор-документа-в-seap</sysRef>
</result>
```

`sysRef` — ключ для отслеживания ответов. Сохраняйте его в БД.

### 5.2. GetNextDocument — получение ответа (polling)

Получает **следующий непрочитанный** документ за последний месяц.

**Параметры:**
- `targetSystem` (опционально) — фильтр по доменной системе, например `SENT`

**Ответ:**
- `document` — полученный документ (content в Base64)
- `hasNext` — `true`, если есть ещё документы

> PUESC также может доставлять ответы через **WS_PUSH**, если зарегистрирован callback URL в SZPROT. Для MVP достаточно polling через `GetNextDocument` / `GetDocuments`.

### 5.3. GetNextDocumentSisc — получение с контекстом компании

Аналог `GetNextDocument`, но:
- без ограничения «1 месяц назад»
- с фильтрацией по `idSiscROF`, `idSiscP`, `idSiscROP` (контекст пользователя/компании/агентства)

### 5.4. GetDocuments — выборка по критериям

Поиск документов по:
- `sysRef` — конкретный документ
- `korelacjaSysref` — **все ответы на отправленный документ** (используйте `sysRef` из AcceptDocument)
- `dataOd` / `dataDo` — диапазон дат (max 10 дней)
- `idWysylki`, `idDocSysZew`

Ограничения:
- max **100** документов за один вызов
- timeout операции: **180 секунд**

---

## 6. Технические сообщения (жизненный цикл)

После отправки RMPD100 SEAP возвращает цепочку **технических** XML-сообщений (схема `schematUPO.xsd`):

| Код | Название | Направление | Описание |
|-----|----------|-------------|----------|
| **UPP** | Urzędowe Poświadczenie Przedłożenia | PUESC → клиент | Документ принят к обработке |
| **NPP** | Poświadczenie Nieprzedłożenia | PUESC → клиент | Документ отклонён (XSD/подпись) |
| **UPO** | Urzędowe Poświadczenie Odbioru | PUESC → клиент | Официальное подтверждение получения |
| **UPD** | Urzędowe Poświadczenie Doręczenia | PUESC → клиент | Требует подписи и возврата в PUESC (14 дней) |
| **PND** | Poświadczenie Niedoręczenia | PUESC → SISC | Если UPD не подписан в срок |

Связь ответов с исходным документом — через атрибут **`KorelacjaSysref`** = `sysRef` отправленного RMPD100.

### Рекомендуемый flow для RMPD100

```
1. Сформировать XML RMPD100 по XSD
2. Подписать XAdES-BES (если требуется)
3. AcceptDocument → получить sysRef
4. Poll GetDocuments(korelacjaSysref=sysRef) или GetNextDocument(targetSystem=SENT)
5. Обработать UPP → UPO → бизнес-ответ SENT (номер референсный)
6. При получении UPD — подписать и вернуть через AcceptDocument
```

---

## 7. Ограничения WebService

| Параметр | Лимит |
|----------|-------|
| Документов XML за запрос | 1 |
| Размер основного XML | 15 MB (включая вложения) |
| Вложений | max 1 |
| Размер вложения | 15 MB |
| ePUAP-подпись | max 10 MB |

---

## 8. Коды ошибок SEAP

| Код | Описание |
|-----|----------|
| **B006** | Нет прав на idSisc / неверные параметры |
| **B010** | SEAP в аварийном режиме |
| **E011** | Превышен лимит одновременных соединений |

Примеры B006:
- `No permission to idSiscROF/idSiscROP/idSiscP`
- `DataOd and DataDo must be defined and must be limited up to 10 days period`
- `At least one of idSisc needs to be provided`

---

## 9. Альтернативные способы отправки (без API)

Если API ещё не реализован, XML RMPD100 можно передать:

1. **Загрузка на портале:** Mój pulpit → Dokumenty do wysyłki → + Nowy dokument → Z PLIKU
2. **Email:** вложение на `puesc@mf.gov.pl` (только с зарегистрированного email)
3. **Интерактивная форма** на портале (RMPD100)

---

## 10. Справочники (słowniki)

Коды стран, типов идентификаторов, видов разрешений и др. — из [словарей PUESC](https://puesc.gov.pl/uslugi/slowniki-przegladanie-i-pobieranie).

API словарей: https://puesc.gov.pl/uslugi/slowniki

Значения полей RMPD проходят системную валидацию — используйте только коды из актуальных словарей.

---

## 11. Help Desk

| Канал | Контакт |
|-------|---------|
| CSD Portal | кнопка «Zgłoś» на puesc.gov.pl |
| Email | helpdesk-eclo@mf.gov.pl (только с email аккаунта PUESC) |
| Тема письма | `Usługi sieciowe SENT` |

Телефонная поддержка для usług sieciowych **не предоставляется**.

---

## 12. Спецификации для скачивания

| Файл | URL |
|------|-----|
| SEAP PLUS PL (PDF) | [puesckud_seap_xml_pl_w_5_50](https://puesc.gov.pl/documents/d/guest/puesckud_seap_xml_pl_w_5_50) |
| SEAP PLUS EN (PDF) | [puesckud_seap_xml_en_w_5_50](https://puesc.gov.pl/documents/d/guest/puesckud_seap_xml_en_w_5_50) |
| Załączniki (WSDL, XSD) | [kanaly-komunikacyjne](https://puesc.gov.pl/uslugi/uslugi-sieciowe-informacje-i-specyfikacje/kanaly-komunikacyjne) |
| RMPD XSD (v20.11.2024) | [system-sent](https://puesc.gov.pl/uslugi/uslugi-sieciowe-informacje-i-specyfikacje/system-sent) |
| DIRECT SENT (альт. канал) | PDF на странице system-sent |

> При расхождении текстовой спецификации и XSD — **приоритет у XSD**.

---

## 13. Пример псевдокода интеграции

```typescript
// Псевдокод — не production-ready

async function submitRmpd100(xmlSigned: string): Promise<string> {
  const passwordDigest = buildPuescPasswordDigest(password, nonce, created);

  const response = await soapClient.AcceptDocument({
    document: {
      content: {
        '@filename': 'rmpd100.xml',
        '@mime': 'application/xml',
        '#text': base64Encode(xmlSigned),
      },
      targetSystems: { system: 'SENT' },
    },
  }, {
    wsSecurity: { username: puescEmail, passwordDigest },
    wsAddressing: { messageId: uuid() },
  });

  return response.result.sysRef; // сохранить для polling
}

async function waitForResponse(sysRef: string): Promise<Document> {
  for (;;) {
    const docs = await soapClient.GetDocuments({
      korelacjaSysref: sysRef,
      dataOd: todayMinusDays(1),
      dataDo: today(),
    });
    const business = docs.find(d => d.type === 'RMPD_RESPONSE');
    if (business) return business;
    await sleep(30_000);
  }
}
```

---

## 14. Чеклист для разработки

- [ ] Скачать `RMPD_v20.11.2024.7z` и `zalaczniki SEAP.zip`
- [ ] Настроить тестовый аккаунт на test.puesc.gov.pl
- [ ] Реализовать WS-Security PasswordDigest
- [ ] Реализовать генерацию XML RMPD100 + XSD-валидацию
- [ ] Реализовать XAdES-BES подпись
- [ ] Реализовать AcceptDocument + GetDocuments polling
- [ ] Обработать UPP/UPO/UPD технические сообщения
- [ ] Интегрировать словари PUESC (коды стран, типы ID)
- [ ] Протестировать на test.puesc.gov.pl
- [ ] Переключить на ws.puesc.gov.pl
