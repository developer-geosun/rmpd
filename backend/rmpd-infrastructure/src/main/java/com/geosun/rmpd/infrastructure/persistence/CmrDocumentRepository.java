package com.geosun.rmpd.infrastructure.persistence;

import com.geosun.rmpd.domain.model.CmrDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CmrDocumentRepository extends JpaRepository<CmrDocument, Long> {

    @Query("SELECT c FROM CmrDocument c WHERE c.declaration.id = :declarationId ORDER BY c.createdAt DESC")
    List<CmrDocument> findByDeclarationIdOrderByCreatedAtDesc(Long declarationId);

    @Query("SELECT c FROM CmrDocument c WHERE c.id = :id AND c.declaration.carrier.id = :carrierId")
    Optional<CmrDocument> findByIdAndCarrierId(Long id, Long carrierId);
}
