package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.api.dto.ApiStubResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings/puesc")
@Tag(name = "PUESC settings", description = "Налаштування PUESC (фаза 2)")
public class PuescSettingsController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "Налаштування PUESC")
    public ResponseEntity<ApiStubResponse> get() {
        return ResponseEntity.ok(ApiStubResponse.notImplemented("GET /settings/puesc"));
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "Тест з'єднання з SEAP")
    public ResponseEntity<ApiStubResponse> test() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiStubResponse.notImplemented("POST /settings/puesc/test"));
    }
}
