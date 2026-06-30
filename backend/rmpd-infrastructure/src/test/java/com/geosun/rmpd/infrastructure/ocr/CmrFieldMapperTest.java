package com.geosun.rmpd.infrastructure.ocr;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CmrFieldMapperTest {

    private final CmrFieldMapper mapper = new CmrFieldMapper(new com.geosun.rmpd.infrastructure.text.TransliterationService());

    @Test
    void mapFromRawText_extractsAtLeastFiveFields() {
        String raw = """
                CMR No: TEST-555
                Sender: ТОВ Сонце
                Receiver: Spedycja Polska
                Place of loading: UA
                Place of delivery: PL
                Date: 01.07.2026
                """;
        OcrExtractionResult result = mapper.mapFromRawText(raw);
        assertTrue(result.fields().size() >= 5);
        assertTrue(result.fields().stream().anyMatch(f -> "cmrNumber".equals(f.fieldKey())));
        assertTrue(result.fields().stream().anyMatch(f -> f.value().matches("(?i)[A-Z0-9\\-/]+")));
    }
}
