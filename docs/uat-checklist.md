# UAT Checklist — Production (фаза 3)

## Перед UAT

- [ ] Staging з `SPRING_PROFILES_ACTIVE=prod` (або test.puesc для dry-run)
- [ ] Credentials PUESC + PKCS12 на staging
- [ ] Backup script перевірений (`backup-mysql` + `restore-mysql`)
- [ ] Prometheus / health dashboard підключений

## Функціональні сценарії

- [ ] Login → створення RMPD100 → validate → download XML
- [ ] Submit на test/prod PUESC → статус REGISTERED + reference number
- [ ] CMR upload → apply ≥ 5 полів
- [ ] Актуалізація REGISTERED декларації → amend XML → submit
- [ ] Email при REGISTERED (якщо `RMPD_MAIL_ENABLED=true`)
- [ ] Multi-tenant: користувач бачить лише свої декларації

## Production критерії

- [ ] `/actuator/health` = UP, dictionary не stale
- [ ] `/actuator/prometheus` доступний для моніторингу
- [ ] Журнал аудиту (`/api/v1/audit`) фіксує submit/amend
- [ ] Словники синхронізовані < 7 днів (`/api/v1/dictionaries/sync-status`)
- [ ] Restore з backup успішний на тестовому MySQL

## Security review

- [ ] JWT secret не дефолтний
- [ ] PUESC паролі зашифровані (AES-GCM)
- [ ] Rate limit на `/submit` та `/cmr/upload`
- [ ] HTTPS на prod frontend + API

## Підпис

| Роль | Ім'я | Дата | Підпис |
|------|------|------|--------|
| QA | | | |
| Product owner | | | |
| DevOps | | | |
