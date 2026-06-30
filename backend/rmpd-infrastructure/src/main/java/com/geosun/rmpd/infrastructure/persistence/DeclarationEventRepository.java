package com.geosun.rmpd.infrastructure.persistence;

import com.geosun.rmpd.domain.model.DeclarationEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeclarationEventRepository extends JpaRepository<DeclarationEvent, Long> {

    @Query("SELECT e FROM DeclarationEvent e WHERE e.declaration.id = :declarationId ORDER BY e.createdAt ASC")
    List<DeclarationEvent> findByDeclarationIdOrderByCreatedAtAsc(Long declarationId);
}
