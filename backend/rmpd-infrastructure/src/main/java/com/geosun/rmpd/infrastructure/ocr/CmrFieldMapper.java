package com.geosun.rmpd.infrastructure.ocr;

import com.geosun.rmpd.infrastructure.text.TransliterationService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Мапінг полів CMR (1–24) → ключі RMPD декларації.
 */
@Component
public class CmrFieldMapper {

    private static final Pattern CMR_NUMBER = Pattern.compile("(?i)CMR\\s*(?:No\\.?|№)?\\s*[:#]?\\s*([A-Z0-9\\-/]+)");
    private static final Pattern DATE = Pattern.compile("\\b(\\d{2}[./-]\\d{2}[./-]\\d{4})\\b");
    private static final Pattern COUNTRY = Pattern.compile("\\b(PL|UA|DE|FR|CZ|SK|LT|LV|EE|RO|HU|IT|ES|BE|NL|AT|BG)\\b");

    private final TransliterationService transliterationService;

    public CmrFieldMapper(TransliterationService transliterationService) {
        this.transliterationService = transliterationService;
    }

    public OcrExtractionResult mapFromRawText(String rawText) {
        String raw = rawText != null ? rawText : "";
        List<ExtractedField> fields = new ArrayList<>();

        add(fields, "cmrNumber", find(CMR_NUMBER, raw), 0.92);
        add(fields, "senderName", extractLine(raw, "Sender", "Nadawca", "Відправник", "1"), 0.78);
        add(fields, "receiverName", extractLine(raw, "Receiver", "Odbiorca", "Отримувач", "2"), 0.76);
        add(fields, "loadingPlace", extractLine(raw, "Place of loading", "Miejsce załadunku", "Місце завантаження", "3"), 0.72);
        add(fields, "unloadingPlace", extractLine(raw, "Place of delivery", "Miejsce dostawy", "Місце розвантаження", "4"), 0.72);
        add(fields, "loadingCountry", extractCountryAfter(raw, "loading", "PL", "UA", "DE"), 0.85);
        add(fields, "unloadingCountry", extractCountryAfter(raw, "unloading", "PL", "DE", "FR"), 0.83);
        add(fields, "routeStartDate", normalizeDate(find(DATE, raw)), 0.70);
        add(fields, "goodsDescription", extractLine(raw, "Nature of goods", "Rodzaj towaru", "Характер вантажу", "11"), 0.65);
        add(fields, "packagesCount", extractLine(raw, "Number of packages", "Liczba opakowań", "Кількість місць", "7"), 0.60);
        add(fields, "grossWeight", extractLine(raw, "Gross weight", "Masa brutto", "Вага брутто", "11"), 0.58);

        return new OcrExtractionResult(raw, fields);
    }

    public OcrExtractionResult mapFromKeyValues(Map<String, String> keyValues, String rawText) {
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : keyValues.entrySet()) {
            normalized.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
        }
        List<ExtractedField> fields = new ArrayList<>();
        mapKv(fields, normalized, "cmr", "cmrNumber", 0.90);
        mapKv(fields, normalized, "sender", "senderName", 0.80);
        mapKv(fields, normalized, "nadawca", "senderName", 0.80);
        mapKv(fields, normalized, "receiver", "receiverName", 0.78);
        mapKv(fields, normalized, "odbiorca", "receiverName", 0.78);
        mapKv(fields, normalized, "loading", "loadingPlace", 0.75);
        mapKv(fields, normalized, "unloading", "unloadingPlace", 0.75);
        mapKv(fields, normalized, "date", "routeStartDate", 0.72);

        OcrExtractionResult fallback = mapFromRawText(rawText);
        for (ExtractedField f : fallback.fields()) {
            if (fields.stream().noneMatch(existing -> existing.fieldKey().equals(f.fieldKey()))) {
                fields.add(f);
            }
        }
        return new OcrExtractionResult(rawText, fields);
    }

    private void mapKv(List<ExtractedField> fields, Map<String, String> kv, String keyPart, String target, double conf) {
        kv.entrySet().stream()
                .filter(e -> e.getKey().contains(keyPart))
                .findFirst()
                .ifPresent(e -> add(fields, target, e.getValue(), conf));
    }

    private void add(List<ExtractedField> fields, String key, String value, double confidence) {
        if (value != null && !value.isBlank()) {
            fields.add(new ExtractedField(key, transliterationService.toLatin(value.trim()), confidence));
        }
    }

    private String find(Pattern pattern, String raw) {
        Matcher m = pattern.matcher(raw);
        return m.find() ? m.group(1) : null;
    }

    private String extractLine(String raw, String... markers) {
        for (String marker : markers) {
            int idx = raw.toLowerCase(Locale.ROOT).indexOf(marker.toLowerCase(Locale.ROOT));
            if (idx >= 0) {
                String tail = raw.substring(idx + marker.length()).trim();
                if (tail.startsWith(":")) {
                    tail = tail.substring(1).trim();
                }
                int end = tail.indexOf('\n');
                return end > 0 ? tail.substring(0, end).trim() : tail;
            }
        }
        return null;
    }

    private String extractCountryAfter(String raw, String context, String... defaults) {
        String lower = raw.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(context);
        if (idx >= 0) {
            Matcher m = COUNTRY.matcher(raw.substring(idx));
            if (m.find()) {
                return m.group(1);
            }
        }
        for (String d : defaults) {
            if (raw.contains(d)) {
                return d;
            }
        }
        return null;
    }

    private String normalizeDate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace('.', '-').replace('/', '-');
        String[] parts = normalized.split("-");
        if (parts.length == 3 && parts[0].length() == 2) {
            return parts[2] + "-" + parts[1] + "-" + parts[0];
        }
        return normalized;
    }
}
