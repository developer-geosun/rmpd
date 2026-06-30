package com.geosun.rmpd.infrastructure.puesc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Обгортка з експоненційним backoff для нестабільних SOAP-викликів SEAP.
 */
@Component
@Primary
public class RetryingPuescSoapClient implements PuescSoapClient {

    private static final Logger log = LoggerFactory.getLogger(RetryingPuescSoapClient.class);

    private final PuescSoapClient delegate;
    private final int maxAttempts;
    private final long initialBackoffMs;

    public RetryingPuescSoapClient(
            @Autowired List<PuescSoapClient> clients,
            @Value("${puesc.retry.max-attempts:3}") int maxAttempts,
            @Value("${puesc.retry.initial-backoff-ms:1000}") long initialBackoffMs) {
        this.delegate = clients.stream()
                .filter(c -> !(c instanceof RetryingPuescSoapClient))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PUESC SOAP delegate not found"));
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialBackoffMs = Math.max(100, initialBackoffMs);
    }

    @Override
    public ConnectionTestResult testConnection(String username, String password) {
        return executeWithRetry(() -> delegate.testConnection(username, password));
    }

    @Override
    public AcceptDocumentResult acceptDocument(String username, String password, byte[] xmlContent, String filename) {
        return executeWithRetry(() -> delegate.acceptDocument(username, password, xmlContent, filename));
    }

    @Override
    public PuescIncomingDocument getNextDocument(String username, String password, String targetSystem) {
        return executeWithRetry(() -> delegate.getNextDocument(username, password, targetSystem));
    }

    @Override
    public PuescIncomingDocument getNextDocumentSisc(
            String username, String password, String targetSystem, SiscContext context) {
        return executeWithRetry(() -> delegate.getNextDocumentSisc(username, password, targetSystem, context));
    }

    private <T> T executeWithRetry(RetryableCall<T> call) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.run();
            } catch (RuntimeException ex) {
                last = ex;
                if (attempt >= maxAttempts || !isRetryable(ex)) {
                    throw ex;
                }
                long delay = initialBackoffMs * (1L << (attempt - 1));
                log.warn("PUESC SOAP attempt {}/{} failed: {} — retry in {}ms", attempt, maxAttempts, ex.getMessage(), delay);
                sleep(delay);
            }
        }
        throw last;
    }

    private boolean isRetryable(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        return msg.contains("timeout")
                || msg.contains("timed out")
                || msg.contains("connection")
                || msg.contains("503")
                || msg.contains("502")
                || msg.contains("unavailable");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Перервано під час очікування retry", ie);
        }
    }

    @FunctionalInterface
    private interface RetryableCall<T> {
        T run();
    }
}
