package com.geosun.rmpd.application.validation;

import java.util.regex.Pattern;

public final class LatinInputValidator {

    private static final Pattern LATIN = Pattern.compile("^[\\p{IsLatin}\\p{Digit}\\s\\-.,'/()&+]*$");

    private LatinInputValidator() {}

    public static boolean isLatin(String value) {
        return value == null || value.isBlank() || LATIN.matcher(value).matches();
    }

    public static String requireLatin(String value, String fieldName) {
        if (!isLatin(value)) {
            throw new IllegalArgumentException(fieldName + " має містити лише латиницю");
        }
        return value;
    }
}
