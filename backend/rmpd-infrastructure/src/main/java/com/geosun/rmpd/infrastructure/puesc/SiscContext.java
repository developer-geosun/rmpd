package com.geosun.rmpd.infrastructure.puesc;

public record SiscContext(String idSiscRop, String idSiscRof, String idSiscP) {

    public static SiscContext of(String idSiscRop, String idSiscRof, String idSiscP) {
        return new SiscContext(idSiscRop, idSiscRof, idSiscP);
    }

    public static SiscContext empty() {
        return new SiscContext(null, null, null);
    }

    public boolean hasAny() {
        return isSet(idSiscRop) || isSet(idSiscRof) || isSet(idSiscP);
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
