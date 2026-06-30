package com.geosun.rmpd.application.dto;

import com.geosun.rmpd.domain.enums.DeclarationStatus;
import com.geosun.rmpd.domain.enums.TransportType;
import java.time.Instant;
import java.time.LocalDate;

public record DeclarationDto(
        Long id,
        DeclarationStatus status,
        TransportType transportType,
        String cmrNumber,
        LocalDate routeStartDate,
        LocalDate routeEndDate,
        String loadingCountry,
        String unloadingCountry,
        Long vehicleId,
        Long permitId,
        Long senderPartyId,
        Long receiverPartyId,
        String routePointsJson,
        String puescSysRef,
        String referenceNumber,
        String comment,
        Instant createdAt,
        Instant updatedAt) {}
