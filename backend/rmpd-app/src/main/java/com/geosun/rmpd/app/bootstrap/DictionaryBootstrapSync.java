package com.geosun.rmpd.app.bootstrap;

import com.geosun.rmpd.application.service.DictionarySyncService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DictionaryBootstrapSync implements ApplicationRunner {

    private final DictionarySyncService dictionarySyncService;

    public DictionaryBootstrapSync(DictionarySyncService dictionarySyncService) {
        this.dictionarySyncService = dictionarySyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        dictionarySyncService.syncAll();
    }
}
