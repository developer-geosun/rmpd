package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.api.audit.Audited;
import com.geosun.rmpd.application.dto.DeclarationDto;
import com.geosun.rmpd.application.dto.DeclarationEventDto;
import com.geosun.rmpd.application.dto.DeclarationProgressDto;
import com.geosun.rmpd.application.dto.DeclarationUpsertDto;
import com.geosun.rmpd.application.dto.SubmitResultDto;
import com.geosun.rmpd.application.dto.ValidationResultDto;
import com.geosun.rmpd.application.service.DeclarationService;
import com.geosun.rmpd.application.service.DeclarationSubmitService;
import com.geosun.rmpd.application.service.PuescPollingService;
import com.geosun.rmpd.domain.enums.DeclarationStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/declarations")
@Tag(name = "Declarations", description = "Декларації RMPD100")
public class DeclarationController {

    private final DeclarationService declarationService;
    private final DeclarationSubmitService submitService;
    private final PuescPollingService pollingService;

    public DeclarationController(
            DeclarationService declarationService,
            DeclarationSubmitService submitService,
            PuescPollingService pollingService) {
        this.declarationService = declarationService;
        this.submitService = submitService;
        this.pollingService = pollingService;
    }

    @GetMapping
    @Operation(summary = "Список декларацій")
    public ResponseEntity<List<DeclarationDto>> list(@RequestParam(required = false) DeclarationStatus status) {
        return ResponseEntity.ok(declarationService.list(status));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Audited(action = "DECLARATION_CREATE", resourceType = "declaration")
    @Operation(summary = "Створити черновик")
    public ResponseEntity<DeclarationDto> create() {
        return ResponseEntity.ok(declarationService.create());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Деталі декларації")
    public ResponseEntity<DeclarationDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(declarationService.get(id));
    }

    @GetMapping("/{id}/progress")
    @Operation(summary = "Прогрес заповнення декларації")
    public ResponseEntity<DeclarationProgressDto> progress(@PathVariable Long id) {
        return ResponseEntity.ok(declarationService.progress(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "Оновити черновик")
    public ResponseEntity<DeclarationDto> update(@PathVariable Long id, @Valid @RequestBody DeclarationUpsertDto dto) {
        return ResponseEntity.ok(declarationService.update(id, dto));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "XSD + бізнес-валідація")
    public ResponseEntity<ValidationResultDto> validate(@PathVariable Long id) {
        return ResponseEntity.ok(declarationService.validate(id));
    }

    @GetMapping("/{id}/xml")
    @Operation(summary = "Завантажити XML")
    public ResponseEntity<byte[]> downloadXml(@PathVariable Long id) {
        byte[] xml = declarationService.generateXml(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rmpd100-" + id + ".xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Audited(action = "DECLARATION_SUBMIT", resourceType = "declaration")
    @Operation(summary = "Підписати та відправити в PUESC")
    public ResponseEntity<SubmitResultDto> submit(@PathVariable Long id) {
        return ResponseEntity.ok(submitService.submit(id));
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Історія статусів")
    public ResponseEntity<List<DeclarationEventDto>> events(@PathVariable Long id) {
        return ResponseEntity.ok(declarationService.listEvents(id));
    }

    @PostMapping("/{id}/copy")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "Створити копію")
    public ResponseEntity<DeclarationDto> copy(@PathVariable Long id) {
        return ResponseEntity.ok(declarationService.copy(id));
    }

    @PostMapping("/{id}/poll")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "Опитати PUESC щодо статусу")
    public ResponseEntity<DeclarationDto> poll(@PathVariable Long id) {
        declarationService.get(id);
        pollingService.pollSubmittedDeclarations();
        return ResponseEntity.ok(declarationService.get(id));
    }
}
