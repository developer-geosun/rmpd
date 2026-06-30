package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.application.dto.AmendmentRequestDto;
import com.geosun.rmpd.application.dto.AuditLogDto;
import com.geosun.rmpd.application.dto.DeclarationDto;
import com.geosun.rmpd.application.dto.DictionarySyncStatusDto;
import com.geosun.rmpd.application.dto.SubmitResultDto;
import com.geosun.rmpd.application.service.AuditQueryService;
import com.geosun.rmpd.application.service.DeclarationAmendmentService;
import com.geosun.rmpd.application.service.DictionarySyncService;
import com.geosun.rmpd.api.audit.Audited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Admin", description = "Адміністрування та production")
public class AdminController {

    private final AuditQueryService auditQueryService;
    private final DictionarySyncService dictionarySyncService;
    private final DeclarationAmendmentService amendmentService;

    public AdminController(
            AuditQueryService auditQueryService,
            DictionarySyncService dictionarySyncService,
            DeclarationAmendmentService amendmentService) {
        this.auditQueryService = auditQueryService;
        this.dictionarySyncService = dictionarySyncService;
        this.amendmentService = amendmentService;
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Журнал аудиту дій користувачів")
    public ResponseEntity<List<AuditLogDto>> auditLog() {
        return ResponseEntity.ok(auditQueryService.listForCarrier());
    }

    @GetMapping("/dictionaries/sync-status")
    @Operation(summary = "Статус синхронізації словників PUESC")
    public ResponseEntity<List<DictionarySyncStatusDto>> dictionarySyncStatus() {
        return ResponseEntity.ok(dictionarySyncService.status());
    }

    @PostMapping("/dictionaries/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "DICTIONARY_SYNC", resourceType = "dictionary")
    @Operation(summary = "Примусова синхронізація словників")
    public ResponseEntity<List<DictionarySyncStatusDto>> syncDictionaries() {
        dictionarySyncService.syncAll();
        return ResponseEntity.ok(dictionarySyncService.status());
    }

    @PutMapping("/declarations/{id}/amend")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Audited(action = "DECLARATION_AMEND", resourceType = "declaration")
    @Operation(summary = "Актуалізація RMPD (зміна даних зареєстрованої декларації)")
    public ResponseEntity<DeclarationDto> amend(@PathVariable Long id, @Valid @RequestBody AmendmentRequestDto dto) {
        return ResponseEntity.ok(amendmentService.applyAmendment(id, dto));
    }

    @GetMapping("/declarations/{id}/amend/xml")
    @Operation(summary = "XML актуалізації RMPD")
    public ResponseEntity<byte[]> amendXml(@PathVariable Long id) {
        byte[] xml = amendmentService.generateAmendmentXml(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rmpd-amend-" + id + ".xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @PostMapping("/declarations/{id}/amend/submit")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Audited(action = "DECLARATION_AMEND_SUBMIT", resourceType = "declaration")
    @Operation(summary = "Відправити актуалізацію в PUESC")
    public ResponseEntity<SubmitResultDto> submitAmendment(
            @PathVariable Long id, @Valid @RequestBody AmendmentRequestDto dto) {
        return ResponseEntity.ok(amendmentService.submitAmendment(id, dto));
    }
}
