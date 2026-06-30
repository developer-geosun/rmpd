package com.geosun.rmpd.application.service;

import com.geosun.rmpd.application.dto.VehicleDto;
import com.geosun.rmpd.application.dto.VehicleUpsertDto;
import com.geosun.rmpd.application.exception.ResourceNotFoundException;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.Vehicle;
import com.geosun.rmpd.infrastructure.persistence.CarrierRepository;
import com.geosun.rmpd.infrastructure.persistence.VehicleRepository;
import com.geosun.rmpd.infrastructure.security.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final CarrierRepository carrierRepository;

    public VehicleService(VehicleRepository vehicleRepository, CarrierRepository carrierRepository) {
        this.vehicleRepository = vehicleRepository;
        this.carrierRepository = carrierRepository;
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> list() {
        return vehicleRepository.findByCarrierIdOrderByTractorNumberAsc(SecurityUtils.requireCarrierId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleDto get(Long id) {
        return toDto(requireVehicle(id));
    }

    @Transactional
    public VehicleDto create(VehicleUpsertDto dto) {
        Vehicle vehicle = new Vehicle();
        vehicle.setCarrier(requireCarrier());
        apply(vehicle, dto);
        return toDto(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleDto update(Long id, VehicleUpsertDto dto) {
        Vehicle vehicle = requireVehicle(id);
        apply(vehicle, dto);
        return toDto(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = requireVehicle(id);
        vehicleRepository.delete(vehicle);
    }

    private Vehicle requireVehicle(Long id) {
        return vehicleRepository.findByIdAndCarrierId(id, SecurityUtils.requireCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("ТЗ не знайдено: " + id));
    }

    private Carrier requireCarrier() {
        Long carrierId = SecurityUtils.requireCarrierId();
        return carrierRepository.findById(carrierId)
                .orElseThrow(() -> new ResourceNotFoundException("Перевізника не знайдено"));
    }

    private void apply(Vehicle vehicle, VehicleUpsertDto dto) {
        vehicle.setRegistrationCountry(dto.registrationCountry().toUpperCase());
        vehicle.setTractorNumber(dto.tractorNumber().toUpperCase());
        vehicle.setTrailerNumber(dto.trailerNumber() != null ? dto.trailerNumber().toUpperCase() : null);
        vehicle.setGpsDeviceId(dto.gpsDeviceId().toUpperCase());
    }

    private VehicleDto toDto(Vehicle vehicle) {
        return new VehicleDto(
                vehicle.getId(),
                vehicle.getRegistrationCountry(),
                vehicle.getTractorNumber(),
                vehicle.getTrailerNumber(),
                vehicle.getGpsDeviceId(),
                vehicle.getCreatedAt());
    }
}
