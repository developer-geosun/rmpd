package com.geosun.rmpd.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VehicleUpsertDto(
        @NotBlank @Size(min = 2, max = 2) String registrationCountry,
        @NotBlank @Size(max = 20) String tractorNumber,
        @Size(max = 20) String trailerNumber,
        @NotBlank @Pattern(regexp = "^[ZM][A-Z]{2}-[A-Z0-9]{6}-[A-Z0-9]$") String gpsDeviceId
) {
}
