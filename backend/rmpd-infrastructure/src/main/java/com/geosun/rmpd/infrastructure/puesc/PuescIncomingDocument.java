package com.geosun.rmpd.infrastructure.puesc;

import java.util.Optional;

public record PuescIncomingDocument(
        String sysRef,
        String documentType,
        String referenceNumber,
        boolean rejected,
        String rawXml) {

    public Optional<String> referenceNumberOpt() {
        return Optional.ofNullable(referenceNumber);
    }
}
