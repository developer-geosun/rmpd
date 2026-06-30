package com.geosun.rmpd.infrastructure.persistence;

import com.geosun.rmpd.domain.model.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarrierRepository extends JpaRepository<Carrier, Long> {
}
