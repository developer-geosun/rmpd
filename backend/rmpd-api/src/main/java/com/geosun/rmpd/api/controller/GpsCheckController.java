package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.application.dto.GpsCheckResultDto;
import com.geosun.rmpd.application.dto.SubmitResultDto;
import com.geosun.rmpd.application.service.GpsCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/declarations/{declarationId}/gps-check")
@Tag(name = "GPS", description = "RMPD406 — перевірка GPS-локатора")
public class GpsCheckController {

    private final GpsCheckService gpsCheckService;

    public GpsCheckController(GpsCheckService gpsCheckService) {
        this.gpsCheckService = gpsCheckService;
    }

    @GetMapping
    @Operation(summary = "Перевірити останню GPS-позицію")
    public ResponseEntity<GpsCheckResultDto> check(@PathVariable Long declarationId) {
        return ResponseEntity.ok(gpsCheckService.check(declarationId));
    }

    @GetMapping("/xml")
    @Operation(summary = "XML RMPD406")
    public ResponseEntity<byte[]> xml(@PathVariable Long declarationId) {
        byte[] xml = gpsCheckService.generateXml(declarationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rmpd406-" + declarationId + ".xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "Відправити RMPD406 в PUESC")
    public ResponseEntity<SubmitResultDto> submit(@PathVariable Long declarationId) {
        return ResponseEntity.ok(gpsCheckService.submitCheck(declarationId));
    }
}
