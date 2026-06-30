package com.geosun.rmpd.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.geosun.rmpd.application.validation.LatinInputValidator;
import com.geosun.rmpd.infrastructure.text.TransliterationService;
import org.junit.jupiter.api.Test;

class TransliterationServiceTest {

    private final TransliterationService service = new TransliterationService();

    @Test
    void transliteratesCyrillicToLatin() {
        String result = service.toLatin("ТОВ Логістика Сонце");
        assertFalse(result.contains("Т"));
        assertEquals(true, LatinInputValidator.isLatin(result));
    }
}
