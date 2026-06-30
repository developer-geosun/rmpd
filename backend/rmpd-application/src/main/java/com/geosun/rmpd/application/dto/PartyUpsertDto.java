package com.geosun.rmpd.application.dto;

import com.geosun.rmpd.domain.enums.PartyRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PartyUpsertDto(
        @NotNull PartyRole partyRole,
        @NotBlank @Size(max = 20) String idType,
        @NotBlank @Size(max = 50) String idNumber,
        @NotBlank @Size(max = 255) String name,
        @Valid AddressDto address
) {
}
