package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.application.dto.CmrApplyRequestDto;
import com.geosun.rmpd.application.dto.CmrDocumentDto;
import com.geosun.rmpd.application.dto.CmrPartySuggestionsDto;
import com.geosun.rmpd.application.dto.CmrUploadCommand;
import com.geosun.rmpd.application.dto.DeclarationDto;
import com.geosun.rmpd.application.service.CmrService;
import com.geosun.rmpd.application.service.DeclarationService;
import com.geosun.rmpd.application.service.PartySuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/declarations/{declarationId}/cmr")
@Tag(name = "CMR", description = "Завантаження та OCR CMR")
public class CmrController {

    private final CmrService cmrService;
    private final DeclarationService declarationService;
    private final PartySuggestionService partySuggestionService;

    public CmrController(
            CmrService cmrService,
            DeclarationService declarationService,
            PartySuggestionService partySuggestionService) {
        this.cmrService = cmrService;
        this.declarationService = declarationService;
        this.partySuggestionService = partySuggestionService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "Завантажити скан CMR")
    public ResponseEntity<CmrDocumentDto> upload(
            @PathVariable Long declarationId,
            @RequestPart("file") MultipartFile file) throws IOException {
        CmrUploadCommand command = new CmrUploadCommand(
                file.getBytes(),
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "cmr.bin",
                file.getContentType(),
                file.getSize());
        return ResponseEntity.ok(cmrService.upload(declarationId, command));
    }

    @GetMapping
    @Operation(summary = "Метадані та розпізнані поля CMR")
    public ResponseEntity<CmrDocumentDto> get(@PathVariable Long declarationId) {
        return ResponseEntity.ok(cmrService.getLatest(declarationId));
    }

    @GetMapping("/preview")
    @Operation(summary = "Перегляд завантаженого скану CMR")
    public ResponseEntity<byte[]> preview(@PathVariable Long declarationId) throws IOException {
        CmrDocumentDto meta = cmrService.getLatest(declarationId);
        byte[] content = cmrService.readPreview(declarationId);
        MediaType mediaType = meta.mimeType() != null
                ? MediaType.parseMediaType(meta.mimeType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + meta.originalFilename() + "\"")
                .contentType(mediaType)
                .body(content);
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Operation(summary = "Застосувати вибрані поля до декларації")
    public ResponseEntity<DeclarationDto> apply(
            @PathVariable Long declarationId,
            @Valid @RequestBody CmrApplyRequestDto request) {
        cmrService.applyFields(declarationId, request);
        return ResponseEntity.ok(declarationService.get(declarationId));
    }

    @GetMapping("/party-suggestions")
    @Operation(summary = "Підказки контрагентів з розпізнаного CMR")
    public ResponseEntity<CmrPartySuggestionsDto> partySuggestions(@PathVariable Long declarationId) {
        return ResponseEntity.ok(partySuggestionService.fromCmr(declarationId));
    }
}
