package com.geosun.rmpd.app.config;

import com.geosun.rmpd.application.dto.DictionarySyncStatusDto;
import com.geosun.rmpd.application.service.DictionarySyncService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rmpd.dictionary-sync.enabled", havingValue = "true", matchIfMissing = true)
public class DictionaryHealthIndicator implements HealthIndicator {

    private final DictionarySyncService dictionarySyncService;

    public DictionaryHealthIndicator(DictionarySyncService dictionarySyncService) {
        this.dictionarySyncService = dictionarySyncService;
    }

    @Override
    public Health health() {
        boolean stale = dictionarySyncService.status().stream().anyMatch(DictionarySyncStatusDto::stale);
        if (stale) {
            return Health.down().withDetail("reason", "Dictionary cache older than 7 days (B010)").build();
        }
        return Health.up().build();
    }
}
