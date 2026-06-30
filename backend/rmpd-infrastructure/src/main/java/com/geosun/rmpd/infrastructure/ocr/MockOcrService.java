package com.geosun.rmpd.infrastructure.ocr;

import com.geosun.rmpd.infrastructure.text.TransliterationService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MockOcrService implements OcrService {

    private static final Pattern CMR_NUMBER = Pattern.compile("(?i)CMR\\s*(?:No\\.?|№)?\\s*[:#]?\\s*([A-Z0-9\\-/]+)");
    private static final Pattern DATE = Pattern.compile("\\b(\\d{2}[./-]\\d{2}[./-]\\d{4})\\b");

    private final TransliterationService transliterationService;

    public MockOcrService(TransliterationService transliterationService) {
        this.transliterationService = transliterationService;
    }

    @Override
    public OcrExtractionResult extract(byte[] fileContent, String mimeType, String filename) {
        String raw = new String(fileContent, StandardCharsets.UTF_8);
        if (!isPrintable(raw)) {
            raw = buildSyntheticText(filename);
        }
        List<ExtractedField> fields = new ArrayList<>();
        addIfPresent(fields, "cmrNumber", find(CMR_NUMBER, raw), 0.92);
        addIfPresent(fields, "senderName", extractLine(raw, "Sender", "Nadawca", "Відправник"), 0.78);
        addIfPresent(fields, "receiverName", extractLine(raw, "Receiver", "Odbiorca", "Отримувач"), 0.76);
        addIfPresent(fields, "loadingCountry", extractCountry(raw, "PL", "UA", "DE"), 0.85);
        addIfPresent(fields, "unloadingCountry", extractCountry(raw, "PL", "DE", "FR"), 0.83);
        addIfPresent(fields, "routeStartDate", normalizeDate(find(DATE, raw)), 0.70);
        return new OcrExtractionResult(raw, fields);
    }

    private boolean isPrintable(String raw) {
        long printable = raw.chars().filter(ch -> ch >= 32 && ch < 127 || Character.isLetter(ch)).count();
        return printable > raw.length() / 4;
    }

    private String buildSyntheticText(String filename) {
        return """
                CMR No: DEMO-12345
                Sender: ТОВ Логістика Сонце
                Receiver: Spedycja Polska Sp. z o.o.
                Loading: UA
                Unloading: PL
                Date: 15.06.2026
                File: %s
                """.formatted(filename);
    }

    private void addIfPresent(List<ExtractedField> fields, String key, String value, double confidence) {
        if (value != null && !value.isBlank()) {
            fields.add(new ExtractedField(key, transliterationService.toLatin(value.trim()), confidence));
        }
    }

    private static String find(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String extractLine(String text, String... markers) {
        for (String marker : markers) {
            int idx = text.toLowerCase().indexOf(marker.toLowerCase());
            if (idx >= 0) {
                int start = idx + marker.length();
                int end = text.indexOf('\n', start);
                if (end < 0) {
                    end = Math.min(text.length(), start + 80);
                }
                String line = text.substring(start, end).replace(":", " ").trim();
                if (!line.isBlank()) {
                    return line;
                }
            }
        }
        return null;
    }

    private static String extractCountry(String text, String... countries) {
        for (String country : countries) {
            if (text.contains(country)) {
                return country;
            }
        }
        return null;
    }

    private static String normalizeDate(String value) {
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
