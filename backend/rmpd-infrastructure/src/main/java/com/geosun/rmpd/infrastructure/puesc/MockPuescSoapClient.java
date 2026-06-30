package com.geosun.rmpd.infrastructure.puesc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "puesc.client", havingValue = "mock", matchIfMissing = true)
public class MockPuescSoapClient implements PuescSoapClient {

    private static final Logger log = LoggerFactory.getLogger(MockPuescSoapClient.class);

    private final Map<String, PendingResponse> pending = new ConcurrentHashMap<>();

    @Override
    public ConnectionTestResult testConnection(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return new ConnectionTestResult(false, "Логін або пароль не вказано", true);
        }
        log.info("Mock PUESC test connection for user={}", username);
        return new ConnectionTestResult(true, "Mock: з'єднання з te-ws.puesc.gov.pl OK", true);
    }

    @Override
    public AcceptDocumentResult acceptDocument(String username, String password, byte[] xmlContent, String filename) {
        String sysRef = "MOCK-" + UUID.randomUUID();
        String refNumber = "SENT-" + System.currentTimeMillis() % 1_000_000_000L;
        pending.put(sysRef, new PendingResponse(refNumber));
        log.info("Mock AcceptDocument sysRef={} user={} filename={} bytes={}", sysRef, username, filename, xmlContent.length);
        return new AcceptDocumentResult(sysRef, true);
    }

    @Override
    public PuescIncomingDocument getNextDocument(String username, String password, String targetSystem) {
        if (pending.isEmpty()) {
            return null;
        }
        Map.Entry<String, PendingResponse> entry = pending.entrySet().iterator().next();
        pending.remove(entry.getKey());
        String sysRef = entry.getKey();
        String ref = entry.getValue().referenceNumber();
        String xml = """
                <RMPD_RESPONSE>
                  <sysRef>%s</sysRef>
                  <referenceNumber>%s</referenceNumber>
                  <status>REGISTERED</status>
                </RMPD_RESPONSE>
                """.formatted(sysRef, ref);
        log.info("Mock GetNextDocument sysRef={} ref={}", sysRef, ref);
        return new PuescIncomingDocument(sysRef, "RMPD_RESPONSE", ref, false, xml);
    }

    private record PendingResponse(String referenceNumber) {}
}
