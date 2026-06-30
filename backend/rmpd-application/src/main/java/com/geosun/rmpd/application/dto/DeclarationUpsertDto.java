package com.geosun.rmpd.application.dto;

import com.geosun.rmpd.domain.enums.TransportType;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DeclarationUpsertDto(
        TransportType transportType,
        @Size(max = 50) String cmrNumber,
        LocalDate routeStartDate,
        LocalDate routeEndDate,
        @Size(min = 2, max = 2) String loadingCountry,
        @Size(min = 2, max = 2) String unloadingCountry,
        Long vehicleId,
        Long permitId,
        Long senderPartyId,
        Long receiverPartyId,
        String routePointsJson,
        String comment,
        Boolean termsAccepted) {}
