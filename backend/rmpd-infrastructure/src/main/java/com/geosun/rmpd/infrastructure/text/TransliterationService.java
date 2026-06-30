package com.geosun.rmpd.infrastructure.text;

import com.ibm.icu.text.Transliterator;
import org.springframework.stereotype.Component;

@Component
public class TransliterationService {

    private final Transliterator cyrillicToLatin = Transliterator.getInstance("Any-Latin; Latin-ASCII");

    public String toLatin(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        return cyrillicToLatin.transliterate(input);
    }
}
