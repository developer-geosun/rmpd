package com.geosun.rmpd.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PermitUpsertDto(
        @NotBlank @Size(max = 50) String permitType,
        @NotBlank @Size(max = 100) String permitNumber,
        @NotNull LocalDate validUntil
) {
}
