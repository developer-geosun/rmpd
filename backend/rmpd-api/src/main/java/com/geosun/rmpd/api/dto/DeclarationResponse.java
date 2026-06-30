package com.geosun.rmpd.api.dto;

import com.geosun.rmpd.domain.enums.DeclarationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Декларація RMPD100")
public record DeclarationResponse(
        Long id,
        DeclarationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
