package com.geosun.rmpd.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.DictionarySyncStatusDto;
import com.geosun.rmpd.domain.model.DictionaryCache;
import com.geosun.rmpd.infrastructure.persistence.DictionaryCacheRepository;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictionarySyncService {

    private static final Logger log = LoggerFactory.getLogger(DictionarySyncService.class);

    private final DictionaryCacheRepository dictionaryCacheRepository;
    private final ObjectMapper objectMapper;

    public DictionarySyncService(DictionaryCacheRepository dictionaryCacheRepository, ObjectMapper objectMapper) {
        this.dictionaryCacheRepository = dictionaryCacheRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void syncAll() {
        syncType("country", "dictionaries/countries.json");
        syncType("id_type", "dictionaries/id_types.json");
        log.info("Dictionary sync completed");
    }

    @Transactional(readOnly = true)
    public List<DictionarySyncStatusDto> status() {
        return List.of(
                statusFor("country"),
                statusFor("id_type"));
    }

    private DictionarySyncStatusDto statusFor(String type) {
        Instant syncedAt = dictionaryCacheRepository.findLatestSyncAt(type);
        long count = dictionaryCacheRepository.findByDictTypeOrderByCodeAsc(type).size();
        boolean stale = syncedAt == null || syncedAt.isBefore(Instant.now().minusSeconds(7L * 24 * 3600));
        return new DictionarySyncStatusDto(type, count, syncedAt, stale);
    }

    private void syncType(String dictType, String classpathResource) {
        try (InputStream in = new ClassPathResource(classpathResource).getInputStream()) {
            List<Map<String, String>> entries = objectMapper.readValue(in, new TypeReference<>() {});
            Instant now = Instant.now();
            for (Map<String, String> entry : entries) {
                String code = entry.get("code");
                DictionaryCache row = dictionaryCacheRepository
                        .findByDictTypeAndCode(dictType, code)
                        .orElseGet(DictionaryCache::new);
                row.setDictType(dictType);
                row.setCode(code);
                row.setLabelPl(entry.get("labelPl"));
                row.setLabelEn(entry.get("labelEn"));
                row.setSyncedAt(now);
                dictionaryCacheRepository.save(row);
            }
            log.info("Synced {} entries for dict_type={}", entries.size(), dictType);
        } catch (Exception ex) {
            throw new IllegalStateException("Помилка синхронізації словника " + dictType, ex);
        }
    }
}
