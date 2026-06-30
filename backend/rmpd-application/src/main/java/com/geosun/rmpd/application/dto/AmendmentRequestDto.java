package com.geosun.rmpd.application.dto;

import java.time.LocalDate;

public record AmendmentRequestDto(
        Long vehicleId,
        LocalDate routeStartDate,
        LocalDate routeEndDate,
        String comment,
        String amendmentReason) {}
