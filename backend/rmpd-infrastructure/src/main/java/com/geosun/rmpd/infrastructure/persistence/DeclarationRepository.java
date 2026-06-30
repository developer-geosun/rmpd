package com.geosun.rmpd.infrastructure.persistence;

import com.geosun.rmpd.domain.enums.DeclarationStatus;
import com.geosun.rmpd.domain.model.Declaration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeclarationRepository extends JpaRepository<Declaration, Long> {

    @Query("SELECT d FROM Declaration d WHERE d.carrier.id = :carrierId ORDER BY d.updatedAt DESC")
    List<Declaration> findByCarrierIdOrderByUpdatedAtDesc(Long carrierId);

    @Query("SELECT d FROM Declaration d WHERE d.id = :id AND d.carrier.id = :carrierId")
    Optional<Declaration> findByIdAndCarrierId(Long id, Long carrierId);

    Optional<Declaration> findByPuescSysRef(String puescSysRef);

    @Query("SELECT d FROM Declaration d WHERE d.status = :status AND d.puescSysRef IS NOT NULL")
    List<Declaration> findByStatusAndPuescSysRefNotNull(DeclarationStatus status);
}
