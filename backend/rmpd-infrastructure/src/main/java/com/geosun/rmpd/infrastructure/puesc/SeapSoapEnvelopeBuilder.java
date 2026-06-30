package com.geosun.rmpd.infrastructure.puesc;

import java.util.Base64;
import java.util.UUID;

public final class SeapSoapEnvelopeBuilder {

    private SeapSoapEnvelopeBuilder() {}

    public static String buildAcceptDocument(
            String username,
            WsSecurityPasswordDigest.DigestResult digest,
            String messageId,
            String base64Content,
            String filename,
            String targetSystem) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:wsa="http://www.w3.org/2005/08/addressing"
                                  xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
                                  xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
                                  xmlns:doc="http://puesc.gov.pl/seap/wsChannel/documentHandling">
                  <soapenv:Header>
                    <wsa:MessageID>%s</wsa:MessageID>
                    <wsse:Security>
                      <wsse:UsernameToken wsu:Id="UsernameToken-1">
                        <wsse:Username>%s</wsse:Username>
                        <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd#PasswordDigest">%s</wsse:Password>
                        <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">%s</wsse:Nonce>
                        <wsu:Created>%s</wsu:Created>
                      </wsse:UsernameToken>
                    </wsse:Security>
                  </soapenv:Header>
                  <soapenv:Body>
                    <doc:AcceptDocumentRequest>
                      <document>
                        <content filename="%s" mime="application/xml">%s</content>
                        <targetSystems>
                          <system>%s</system>
                        </targetSystems>
                      </document>
                    </doc:AcceptDocumentRequest>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                messageId,
                escapeXml(username),
                digest.passwordDigest(),
                digest.nonce(),
                digest.created(),
                escapeXml(filename),
                base64Content,
                escapeXml(targetSystem));
    }

    public static String buildGetNextDocument(
            String username,
            WsSecurityPasswordDigest.DigestResult digest,
            String messageId,
            String targetSystem) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:wsa="http://www.w3.org/2005/08/addressing"
                                  xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
                                  xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
                                  xmlns:doc="http://puesc.gov.pl/seap/wsChannel/documentHandling">
                  <soapenv:Header>
                    <wsa:MessageID>%s</wsa:MessageID>
                    <wsse:Security>
                      <wsse:UsernameToken wsu:Id="UsernameToken-1">
                        <wsse:Username>%s</wsse:Username>
                        <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd#PasswordDigest">%s</wsse:Password>
                        <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">%s</wsse:Nonce>
                        <wsu:Created>%s</wsu:Created>
                      </wsse:UsernameToken>
                    </wsse:Security>
                  </soapenv:Header>
                  <soapenv:Body>
                    <doc:GetNextDocumentRequest>
                      <targetSystem>%s</targetSystem>
                    </doc:GetNextDocumentRequest>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                messageId,
                escapeXml(username),
                digest.passwordDigest(),
                digest.nonce(),
                digest.created(),
                escapeXml(targetSystem));
    }

    public static String newMessageId() {
        return "urn:uuid:" + UUID.randomUUID();
    }

    public static String toBase64(String xml) {
        return Base64.getEncoder().encodeToString(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
