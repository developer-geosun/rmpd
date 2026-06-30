package com.geosun.rmpd.infrastructure.ocr;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rmpd.ocr.provider", havingValue = "mock", matchIfMissing = true)
public class MockOcrService implements OcrService {

    private final CmrFieldMapper fieldMapper;

    public MockOcrService(CmrFieldMapper fieldMapper) {
        this.fieldMapper = fieldMapper;
    }

    @Override
    public OcrExtractionResult extract(byte[] fileContent, String mimeType, String filename) {
        String raw = new String(fileContent, StandardCharsets.UTF_8);
        if (!isPrintable(raw)) {
            raw = buildSyntheticText(filename);
        }
        return fieldMapper.mapFromRawText(raw);
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
                Place of loading: UA
                Place of delivery: PL
                Date: 15.06.2026
                File: %s
                """.formatted(filename);
    }
}
