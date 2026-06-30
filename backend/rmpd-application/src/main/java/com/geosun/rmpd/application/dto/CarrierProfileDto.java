package com.geosun.rmpd.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CarrierProfileDto(
        @NotBlank @Size(max = 20) String idType,
        @NotBlank @Size(max = 50) String idNumber,
        @NotBlank @Size(max = 255) String name,
        @Valid AddressDto address,
        @NotBlank @Email String email
) {
}
