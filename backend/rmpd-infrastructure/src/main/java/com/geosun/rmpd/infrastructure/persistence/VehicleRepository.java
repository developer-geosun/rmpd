package com.geosun.rmpd.infrastructure.persistence;

import com.geosun.rmpd.domain.model.Vehicle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByCarrierIdOrderByTractorNumberAsc(Long carrierId);

    Optional<Vehicle> findByIdAndCarrierId(Long id, Long carrierId);
}
