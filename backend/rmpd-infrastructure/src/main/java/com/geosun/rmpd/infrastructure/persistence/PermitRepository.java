package com.geosun.rmpd.infrastructure.persistence;

import com.geosun.rmpd.domain.model.Permit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermitRepository extends JpaRepository<Permit, Long> {

    List<Permit> findByCarrierIdOrderByValidUntilDesc(Long carrierId);

    Optional<Permit> findByIdAndCarrierId(Long id, Long carrierId);
}
