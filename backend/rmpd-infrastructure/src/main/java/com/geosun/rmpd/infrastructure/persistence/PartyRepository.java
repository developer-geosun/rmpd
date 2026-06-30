package com.geosun.rmpd.infrastructure.persistence;

import com.geosun.rmpd.domain.model.Party;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyRepository extends JpaRepository<Party, Long> {

    List<Party> findByCarrierIdOrderByNameAsc(Long carrierId);

    Optional<Party> findByIdAndCarrierId(Long id, Long carrierId);

    List<Party> findByCarrierIdAndNameContainingIgnoreCaseOrderByNameAsc(Long carrierId, String name);
}
