package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.application.dto.CarrierProfileDto;
import com.geosun.rmpd.application.service.CarrierSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings/carrier")
@Tag(name = "Carrier settings", description = "Профіль перевізника")
public class CarrierSettingsController {

    private final CarrierSettingsService carrierSettingsService;

    public CarrierSettingsController(CarrierSettingsService carrierSettingsService) {
        this.carrierSettingsService = carrierSettingsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','VIEWER')")
    @Operation(summary = "Отримати профіль перевізника")
    public ResponseEntity<CarrierProfileDto> get() {
        return ResponseEntity.ok(carrierSettingsService.getProfile());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "Оновити профіль перевізника")
    public ResponseEntity<CarrierProfileDto> update(@Valid @RequestBody CarrierProfileDto dto) {
        return ResponseEntity.ok(carrierSettingsService.updateProfile(dto));
    }
}
