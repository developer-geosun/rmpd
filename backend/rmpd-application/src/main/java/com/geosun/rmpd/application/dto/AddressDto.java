package com.geosun.rmpd.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressDto(
        @NotBlank @Size(max = 2) String country,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 200) String street,
        @NotBlank @Size(max = 20) String buildingNumber,
        @Size(max = 20) String unitNumber
) {
}
