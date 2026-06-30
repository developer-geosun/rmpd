package com.geosun.rmpd.infrastructure.puesc;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "puesc.client", havingValue = "http")
public class HttpPuescSoapClient implements PuescSoapClient {

    private static final Logger log = LoggerFactory.getLogger(HttpPuescSoapClient.class);
    private static final Pattern SYS_REF_PATTERN = Pattern.compile("<sysRef>([^<]+)</sysRef>");

    private final String endpointUrl;
    private final String targetSystem;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public HttpPuescSoapClient(
            @Value("${puesc.soap-endpoint:https://te-ws.puesc.gov.pl/seap_wsChannel/DocumentHandlingPort}") String endpointUrl,
            @Value("${puesc.target-system:SENT}") String targetSystem) {
        this.endpointUrl = endpointUrl;
        this.targetSystem = targetSystem;
    }

    @Override
    public ConnectionTestResult testConnection(String username, String password) {
        try {
            String envelope = SeapSoapEnvelopeBuilder.buildGetNextDocument(
                    username,
                    WsSecurityPasswordDigest.create(password),
                    SeapSoapEnvelopeBuilder.newMessageId(),
                    targetSystem);
            HttpResponse<String> response = post(envelope);
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                if (response.body().contains("SecurityError") || response.body().contains("faultstring")) {
                    return new ConnectionTestResult(false, "SEAP: помилка авторизації", false);
                }
                return new ConnectionTestResult(true, "З'єднання з PUESC OK (HTTP " + response.statusCode() + ")", false);
            }
            return new ConnectionTestResult(false, "HTTP " + response.statusCode(), false);
        } catch (Exception ex) {
            log.warn("PUESC test connection failed: {}", ex.getMessage());
            return new ConnectionTestResult(false, ex.getMessage(), false);
        }
    }

    @Override
    public AcceptDocumentResult acceptDocument(String username, String password, byte[] xmlContent, String filename) {
        try {
            String base64 = java.util.Base64.getEncoder().encodeToString(xmlContent);
            String envelope = SeapSoapEnvelopeBuilder.buildAcceptDocument(
                    username,
                    WsSecurityPasswordDigest.create(password),
                    SeapSoapEnvelopeBuilder.newMessageId(),
                    base64,
                    filename,
                    targetSystem);
            HttpResponse<String> response = post(envelope);
            String body = response.body();
            Matcher matcher = SYS_REF_PATTERN.matcher(body);
            if (matcher.find()) {
                String sysRef = matcher.group(1);
                log.info("AcceptDocument sysRef={}", sysRef);
                return new AcceptDocumentResult(sysRef, false);
            }
            throw new IllegalStateException("sysRef не знайдено у відповіді SEAP");
        } catch (Exception ex) {
            throw new IllegalStateException("Помилка AcceptDocument: " + ex.getMessage(), ex);
        }
    }

    @Override
    public PuescIncomingDocument getNextDocument(String username, String password, String targetSystem) {
        try {
            String envelope = SeapSoapEnvelopeBuilder.buildGetNextDocument(
                    username,
                    WsSecurityPasswordDigest.create(password),
                    SeapSoapEnvelopeBuilder.newMessageId(),
                    targetSystem);
            HttpResponse<String> response = post(envelope);
            String body = response.body();
            if (body == null || body.isBlank() || body.contains("No documents")) {
                return null;
            }
            String sysRef = extract(body, SYS_REF_PATTERN);
            String ref = extract(body, Pattern.compile("<referenceNumber>([^<]+)</referenceNumber>"));
            boolean rejected = body.contains("REJECTED") || body.contains("NPP");
            return new PuescIncomingDocument(sysRef, "RMPD_RESPONSE", ref, rejected, body);
        } catch (Exception ex) {
            log.warn("GetNextDocument failed: {}", ex.getMessage());
            return null;
        }
    }

    private HttpResponse<String> post(String envelope) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "text/xml; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String extract(String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }
}
