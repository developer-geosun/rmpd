package com.geosun.rmpd.application.dto;

import com.geosun.rmpd.domain.enums.PuescEnvironment;
import java.time.Instant;

public record PuescCredentialDto(
        PuescEnvironment environment,
        String username,
        boolean passwordConfigured,
        String signingCertPath,
        String idSiscRop,
        String idSiscRof,
        String idSiscP,
        boolean active,
        Instant lastTestAt,
        Boolean lastTestOk) {}
