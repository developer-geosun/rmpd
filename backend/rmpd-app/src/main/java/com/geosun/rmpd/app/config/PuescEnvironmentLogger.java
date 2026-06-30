package com.geosun.rmpd.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PuescEnvironmentLogger {

    private static final Logger log = LoggerFactory.getLogger(PuescEnvironmentLogger.class);

    private final String env;
    private final String client;
    private final String soapEndpoint;
    private final String wsdlUrl;

    public PuescEnvironmentLogger(
            @Value("${puesc.env:test}") String env,
            @Value("${puesc.client:mock}") String client,
            @Value("${puesc.soap-endpoint}") String soapEndpoint,
            @Value("${puesc.wsdl-url}") String wsdlUrl) {
        this.env = env;
        this.client = client;
        this.soapEndpoint = soapEndpoint;
        this.wsdlUrl = wsdlUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logConfiguration() {
        log.info("PUESC env={} client={} endpoint={} wsdl={}", env, client, soapEndpoint, wsdlUrl);
        if ("prod".equalsIgnoreCase(env) && "mock".equalsIgnoreCase(client)) {
            log.warn("PUESC_ENV=prod but PUESC_CLIENT=mock — production submissions are disabled");
        }
    }
}
