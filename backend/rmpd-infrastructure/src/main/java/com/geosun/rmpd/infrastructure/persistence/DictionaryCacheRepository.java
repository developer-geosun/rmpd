package com.geosun.rmpd.infrastructure.persistence;

import com.geosun.rmpd.domain.model.DictionaryCache;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictionaryCacheRepository extends JpaRepository<DictionaryCache, Long> {

    List<DictionaryCache> findByDictTypeOrderByCodeAsc(String dictType);
}
