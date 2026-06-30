package com.geosun.rmpd.infrastructure.persistence;

import com.geosun.rmpd.domain.model.DictionaryCache;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DictionaryCacheRepository extends JpaRepository<DictionaryCache, Long> {

    List<DictionaryCache> findByDictTypeOrderByCodeAsc(String dictType);

    Optional<DictionaryCache> findByDictTypeAndCode(String dictType, String code);

    @Query("SELECT MAX(d.syncedAt) FROM DictionaryCache d WHERE d.dictType = :dictType")
    Instant findLatestSyncAt(@Param("dictType") String dictType);
}
