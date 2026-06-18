# Спецификации PUESC

Здесь размещаются скачанные с портала PUESC технические спецификации. Файлы **не включены в репозиторий** — их нужно загрузить вручную.

## RMPD (бизнес-XML)

1. Перейти: https://puesc.gov.pl/uslugi/uslugi-sieciowe-informacje-i-specyfikacje/system-sent
2. Скачать: **RMPD_v20.11.2024** (архив 7z, ~6.66 MB)
3. Распаковать в `specs/rmpd/RMPD_v20.11.2024/`

## SEAP (транспортный канал)

1. Перейти: https://puesc.gov.pl/uslugi/uslugi-sieciowe-informacje-i-specyfikacje/kanaly-komunikacyjne
2. Скачать:
   - **SEAP PLUS Specyfikacja Techniczna Publiczna PL** (PDF)
   - **Załączniki do Specyfikacji** (zip: WSDL, WS_CHANNEL.xsd, schematUPO.xsd)
3. Распаковать в `specs/seap/`

## Справочные материалы

| Файл | URL |
|------|-----|
| Wykaz zezwoleń RMPD (PL) | Страница услуги RMPD |
| Adresacja PUESC (xlsx) | Страница kanały komunikacyjne |
| DIRECT SENT spec (PDF) | Страница system-sent |

## Версионирование

При обновлении спецификаций на PUESC:
1. Скачать новую версию
2. Зафиксировать дату и версию в этом README
3. Прогнать XSD-валидацию тестовых XML
4. Обновить генератор XML

Текущая актуальная версия RMPD: **v20.11.2024** (на 2024-11-20).
