package com.geosun.rmpd.app.config;

import com.geosun.rmpd.application.service.DictionarySyncService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rmpd.dictionary-sync.enabled", havingValue = "true", matchIfMissing = true)
public class DictionarySyncScheduler {

    private final DictionarySyncService dictionarySyncService;

    public DictionarySyncScheduler(DictionarySyncService dictionarySyncService) {
        this.dictionarySyncService = dictionarySyncService;
    }

    @Scheduled(cron = "${rmpd.dictionary-sync.cron:0 0 3 * * *}")
    public void sync() {
        dictionarySyncService.syncAll();
    }
}
