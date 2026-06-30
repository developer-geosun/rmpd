# Runbook: аварійна подача RMPD

Коли автоматична відправка через SEAP недоступна, використовуйте ручну подачу на PUESC.

## Контакти

- **Аварійна служба GITD:** awaria@gitd.gov.pl
- **PUESC prod:** https://puesc.gov.pl/
- **PUESC test:** https://test.puesc.gov.pl/

## Сценарій 1 — SEAP недоступний, декларація ще не зареєстрована

1. Увійдіть у RMPD → відкрийте декларацію.
2. Натисніть **Перевірити**, потім **Завантажити XML**.
3. Перевірте XML локально (XSD) або через `POST /api/v1/declarations/{id}/validate`.
4. Завантажте XML вручну на test.puesc.gov.pl або prod (залежно від середовища).
5. Зафіксуйте референсний номер у коментарі до декларації та в internal ticket.

## Сценарій 2 — потрібна актуалізація (форма RMPD)

1. Декларація має статус `REGISTERED` і референс SENT.
2. Внесіть зміни в розділі **Актуалізація RMPD**.
3. Завантажте XML актуалізації (`GET .../amend/xml`) або відправте через **Відправити актуалізацію**.
4. Якщо API недоступний — завантажте `rmpd-amend-{id}.xml` вручну на PUESC.

## Сценарій 3 — відновлення після втрати БД

```bash
cd deploy/scripts
./restore-mysql.sh ../backups/rmpd_YYYYMMDD_HHMMSS.sql.gz
```

Перевірте `docker compose ps` і `/actuator/health`.

## Моніторинг

- Health: `GET /actuator/health` (словники — алерт B010 при `stale=true`)
- Метрики: `GET /actuator/prometheus`
- Аудит дій: `GET /api/v1/audit` (роль ADMIN)

## Prod checklist

- `SPRING_PROFILES_ACTIVE=prod`
- `PUESC_ENV=prod`, `PUESC_CLIENT=http`
- `RMPD_SIGNING_CERT_PASSWORD` + шлях P12 у PUESC settings
- `RMPD_JWT_SECRET` — унікальний 256-bit ключ
- Щоденний backup: `deploy/scripts/backup-mysql.sh` (cron)
