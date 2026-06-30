package com.geosun.rmpd.application.dto;

import com.geosun.rmpd.domain.enums.PuescEnvironment;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PuescCredentialUpsertDto(
        PuescEnvironment environment,
        @NotBlank @Email String username,
        @Size(min = 4) String password,
        String signingCertPath,
        @Size(max = 50) String idSiscRop,
        @Size(max = 50) String idSiscRof,
        @Size(max = 50) String idSiscP) {}
