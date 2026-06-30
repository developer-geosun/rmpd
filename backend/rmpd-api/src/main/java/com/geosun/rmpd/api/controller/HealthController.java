package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.api.dto.ApiStubResponse;
import com.geosun.rmpd.api.dto.HealthResponse;
import com.geosun.rmpd.application.service.HealthService;
import com.geosun.rmpd.infrastructure.xml.XsdValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "Перевірка доступності сервісу")
public class HealthController {

    private final HealthService healthService;
    private final XsdValidator xsdValidator;
    private final String version;

    public HealthController(
            HealthService healthService,
            XsdValidator xsdValidator,
            @Value("${rmpd.version:0.1.0-SNAPSHOT}") String version) {
        this.healthService = healthService;
        this.xsdValidator = xsdValidator;
        this.version = version;
    }

    @GetMapping("/health")
    @Operation(summary = "Статус API та наявність XSD")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
                healthService.status(),
                version,
                xsdValidator.isXsdAvailable()));
    }
}
