package com.geosun.rmpd.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Статус API")
public record HealthResponse(
        @Schema(example = "UP") String status,
        @Schema(example = "0.1.0-SNAPSHOT") String version,
        @Schema(description = "Чи доступні XSD-схеми PUESC") boolean xsdAvailable
) {
}
