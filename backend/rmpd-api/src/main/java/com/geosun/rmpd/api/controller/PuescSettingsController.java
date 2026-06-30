package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.application.dto.PuescConnectionTestDto;
import com.geosun.rmpd.application.dto.PuescCredentialDto;
import com.geosun.rmpd.application.dto.PuescCredentialUpsertDto;
import com.geosun.rmpd.application.service.PuescCredentialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings/puesc")
@Tag(name = "PUESC settings", description = "Налаштування PUESC")
public class PuescSettingsController {

    private final PuescCredentialService credentialService;

    public PuescSettingsController(PuescCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "Налаштування PUESC")
    public ResponseEntity<PuescCredentialDto> get() {
        return ResponseEntity.ok(credentialService.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Зберегти credentials PUESC")
    public ResponseEntity<PuescCredentialDto> upsert(@Valid @RequestBody PuescCredentialUpsertDto dto) {
        return ResponseEntity.ok(credentialService.upsert(dto));
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "Тест з'єднання з SEAP")
    public ResponseEntity<PuescConnectionTestDto> test() {
        return ResponseEntity.ok(credentialService.testConnection());
    }
}
