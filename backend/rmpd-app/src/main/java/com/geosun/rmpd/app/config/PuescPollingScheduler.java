package com.geosun.rmpd.app.config;

import com.geosun.rmpd.application.service.PuescPollingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "puesc.polling.enabled", havingValue = "true", matchIfMissing = true)
public class PuescPollingScheduler {

    private final PuescPollingService pollingService;

    public PuescPollingScheduler(PuescPollingService pollingService) {
        this.pollingService = pollingService;
    }

    @Scheduled(fixedDelayString = "${puesc.polling.interval-ms:30000}")
    public void poll() {
        pollingService.pollSubmittedDeclarations();
    }
}
