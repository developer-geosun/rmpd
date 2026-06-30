package com.geosun.rmpd.infrastructure.persistence;

import com.geosun.rmpd.domain.enums.PuescEnvironment;
import com.geosun.rmpd.domain.model.PuescCredential;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PuescCredentialRepository extends JpaRepository<PuescCredential, Long> {

    @Query("SELECT c FROM PuescCredential c WHERE c.carrier.id = :carrierId AND c.environment = :environment")
    Optional<PuescCredential> findByCarrierIdAndEnvironment(Long carrierId, PuescEnvironment environment);

    @Query("SELECT c FROM PuescCredential c WHERE c.carrier.id = :carrierId AND c.active = true")
    Optional<PuescCredential> findActiveByCarrierId(Long carrierId);
}
