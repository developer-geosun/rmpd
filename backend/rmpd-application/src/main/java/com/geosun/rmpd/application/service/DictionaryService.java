package com.geosun.rmpd.application.service;

import com.geosun.rmpd.application.dto.DictionaryEntryDto;
import com.geosun.rmpd.infrastructure.persistence.DictionaryCacheRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictionaryService {

    private final DictionaryCacheRepository dictionaryCacheRepository;

    public DictionaryService(DictionaryCacheRepository dictionaryCacheRepository) {
        this.dictionaryCacheRepository = dictionaryCacheRepository;
    }

    @Transactional(readOnly = true)
    public List<DictionaryEntryDto> getByType(String type) {
        return dictionaryCacheRepository.findByDictTypeOrderByCodeAsc(type).stream()
                .map(d -> new DictionaryEntryDto(d.getCode(), d.getLabelPl(), d.getLabelEn()))
                .toList();
    }
}
