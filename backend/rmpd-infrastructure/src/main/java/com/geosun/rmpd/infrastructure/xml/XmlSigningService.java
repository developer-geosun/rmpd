package com.geosun.rmpd.infrastructure.xml;

import org.springframework.stereotype.Component;

@Component
public class XmlSigningService {

    public byte[] sign(byte[] xmlContent, String signingCertPath) {
        // У dev/mock режимі повертаємо XML без XAdES; для prod — інтеграція EU DSS
        return xmlContent;
    }
}
