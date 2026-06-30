package com.geosun.rmpd.application.service;

import com.geosun.rmpd.application.dto.CarrierProfileDto;
import com.geosun.rmpd.application.exception.ResourceNotFoundException;
import com.geosun.rmpd.application.support.JsonMapper;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.infrastructure.persistence.CarrierRepository;
import com.geosun.rmpd.infrastructure.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarrierSettingsService {

    private final CarrierRepository carrierRepository;
    private final JsonMapper jsonMapper;

    public CarrierSettingsService(CarrierRepository carrierRepository, JsonMapper jsonMapper) {
        this.carrierRepository = carrierRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(readOnly = true)
    public CarrierProfileDto getProfile() {
        return toDto(requireCarrier());
    }

    @Transactional
    public CarrierProfileDto updateProfile(CarrierProfileDto dto) {
        Carrier carrier = requireCarrier();
        carrier.setIdType(dto.idType());
        carrier.setIdNumber(dto.idNumber());
        carrier.setName(dto.name());
        carrier.setEmail(dto.email());
        carrier.setAddressJson(jsonMapper.toJson(dto.address()));
        return toDto(carrierRepository.save(carrier));
    }

    private Carrier requireCarrier() {
        Long carrierId = SecurityUtils.requireCarrierId();
        return carrierRepository.findById(carrierId)
                .orElseThrow(() -> new ResourceNotFoundException("Перевізника не знайдено"));
    }

    private CarrierProfileDto toDto(Carrier carrier) {
        return new CarrierProfileDto(
                carrier.getIdType(),
                carrier.getIdNumber(),
                carrier.getName(),
                jsonMapper.toAddress(carrier.getAddressJson()),
                carrier.getEmail());
    }
}
