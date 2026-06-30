package com.geosun.rmpd.application.service;

import com.geosun.rmpd.application.dto.PermitDto;
import com.geosun.rmpd.application.dto.PermitUpsertDto;
import com.geosun.rmpd.application.exception.ResourceNotFoundException;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.Permit;
import com.geosun.rmpd.infrastructure.persistence.CarrierRepository;
import com.geosun.rmpd.infrastructure.persistence.PermitRepository;
import com.geosun.rmpd.infrastructure.security.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermitService {

    private final PermitRepository permitRepository;
    private final CarrierRepository carrierRepository;

    public PermitService(PermitRepository permitRepository, CarrierRepository carrierRepository) {
        this.permitRepository = permitRepository;
        this.carrierRepository = carrierRepository;
    }

    @Transactional(readOnly = true)
    public List<PermitDto> list() {
        return permitRepository.findByCarrierIdOrderByValidUntilDesc(SecurityUtils.requireCarrierId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermitDto get(Long id) {
        return toDto(requirePermit(id));
    }

    @Transactional
    public PermitDto create(PermitUpsertDto dto) {
        Permit permit = new Permit();
        permit.setCarrier(requireCarrier());
        apply(permit, dto);
        return toDto(permitRepository.save(permit));
    }

    @Transactional
    public PermitDto update(Long id, PermitUpsertDto dto) {
        Permit permit = requirePermit(id);
        apply(permit, dto);
        return toDto(permitRepository.save(permit));
    }

    @Transactional
    public void delete(Long id) {
        permitRepository.delete(requirePermit(id));
    }

    private Permit requirePermit(Long id) {
        return permitRepository.findByIdAndCarrierId(id, SecurityUtils.requireCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("Дозвіл не знайдено: " + id));
    }

    private Carrier requireCarrier() {
        return carrierRepository.findById(SecurityUtils.requireCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("Перевізника не знайдено"));
    }

    private void apply(Permit permit, PermitUpsertDto dto) {
        permit.setPermitType(dto.permitType());
        permit.setPermitNumber(dto.permitNumber());
        permit.setValidUntil(dto.validUntil());
    }

    private PermitDto toDto(Permit permit) {
        return new PermitDto(
                permit.getId(),
                permit.getPermitType(),
                permit.getPermitNumber(),
                permit.getValidUntil(),
                permit.getCreatedAt());
    }
}
