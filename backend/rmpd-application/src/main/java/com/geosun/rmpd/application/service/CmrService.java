package com.geosun.rmpd.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.CmrApplyRequestDto;
import com.geosun.rmpd.application.dto.CmrDocumentDto;
import com.geosun.rmpd.application.dto.CmrExtractedFieldDto;
import com.geosun.rmpd.application.exception.ResourceNotFoundException;
import com.geosun.rmpd.domain.enums.DeclarationEventType;
import com.geosun.rmpd.domain.model.CmrDocument;
import com.geosun.rmpd.domain.model.Declaration;
import com.geosun.rmpd.domain.model.DeclarationEvent;
import com.geosun.rmpd.infrastructure.ocr.ExtractedField;
import com.geosun.rmpd.infrastructure.ocr.OcrExtractionResult;
import com.geosun.rmpd.infrastructure.ocr.OcrService;
import com.geosun.rmpd.infrastructure.persistence.CmrDocumentRepository;
import com.geosun.rmpd.infrastructure.persistence.DeclarationEventRepository;
import com.geosun.rmpd.infrastructure.persistence.DeclarationRepository;
import com.geosun.rmpd.infrastructure.security.SecurityUtils;
import com.geosun.rmpd.infrastructure.storage.LocalFileStorageService;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.geosun.rmpd.application.dto.CmrUploadCommand;

@Service
public class CmrService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME = Set.of(
            "application/pdf", "image/jpeg", "image/png", "text/plain");

    private final DeclarationRepository declarationRepository;
    private final CmrDocumentRepository cmrDocumentRepository;
    private final DeclarationEventRepository eventRepository;
    private final LocalFileStorageService fileStorageService;
    private final OcrService ocrService;
    private final ObjectMapper objectMapper;

    public CmrService(
            DeclarationRepository declarationRepository,
            CmrDocumentRepository cmrDocumentRepository,
            DeclarationEventRepository eventRepository,
            LocalFileStorageService fileStorageService,
            OcrService ocrService,
            ObjectMapper objectMapper) {
        this.declarationRepository = declarationRepository;
        this.cmrDocumentRepository = cmrDocumentRepository;
        this.eventRepository = eventRepository;
        this.fileStorageService = fileStorageService;
        this.ocrService = ocrService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CmrDocumentDto upload(Long declarationId, CmrUploadCommand upload) throws IOException {
        Declaration declaration = requireDeclaration(declarationId);
        if (upload.sizeBytes() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Файл перевищує 10 МБ");
        }
        String mime = upload.mimeType() != null ? upload.mimeType() : "application/octet-stream";
        if (!ALLOWED_MIME.contains(mime)) {
            throw new IllegalArgumentException("Непідтримуваний тип файлу: " + mime);
        }

        byte[] content = upload.content();
        OcrExtractionResult extraction = ocrService.extract(content, mime, upload.originalFilename());
        LocalFileStorageService.StoredFile stored = fileStorageService.store(content, upload.originalFilename());

        CmrDocument doc = new CmrDocument();
        doc.setDeclaration(declaration);
        doc.setFilePath(stored.path());
        doc.setOriginalFilename(upload.originalFilename());
        doc.setMimeType(mime);
        doc.setFileSizeBytes(stored.sizeBytes());
        doc.setOcrRawText(extraction.rawText());
        doc.setExtractedFieldsJson(toJson(extraction.fields()));
        doc = cmrDocumentRepository.save(doc);
        return toDto(doc);
    }

    @Transactional(readOnly = true)
    public CmrDocumentDto getLatest(Long declarationId) {
        requireDeclaration(declarationId);
        return cmrDocumentRepository.findByDeclarationIdOrderByCreatedAtDesc(declarationId).stream()
                .findFirst()
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("CMR не знайдено"));
    }

    @Transactional
    public void applyFields(Long declarationId, CmrApplyRequestDto request) {
        Declaration declaration = requireDeclaration(declarationId);
        CmrDocument doc = cmrDocumentRepository.findByDeclarationIdOrderByCreatedAtDesc(declarationId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CMR не знайдено"));

        List<ExtractedField> fields = fromJson(doc.getExtractedFieldsJson());
        Map<String, String> selected = fields.stream()
                .filter(f -> request.fieldKeys().contains(f.fieldKey()))
                .collect(Collectors.toMap(ExtractedField::fieldKey, ExtractedField::value, (a, b) -> b));

        if (selected.containsKey("cmrNumber")) {
            declaration.setCmrNumber(selected.get("cmrNumber"));
        }
        if (selected.containsKey("loadingCountry")) {
            declaration.setLoadingCountry(selected.get("loadingCountry"));
        }
        if (selected.containsKey("unloadingCountry")) {
            declaration.setUnloadingCountry(selected.get("unloadingCountry"));
        }
        if (selected.containsKey("routeStartDate")) {
            declaration.setRouteStartDate(LocalDate.parse(selected.get("routeStartDate")));
        }

        doc.setAppliedAt(Instant.now());
        cmrDocumentRepository.save(doc);
        declaration = declarationRepository.save(declaration);

        DeclarationEvent event = new DeclarationEvent();
        event.setDeclaration(declaration);
        event.setEventType(DeclarationEventType.CMR_APPLIED);
        event.setPayloadJson(toJson(selected));
        eventRepository.save(event);
    }

    private Declaration requireDeclaration(Long id) {
        return declarationRepository.findByIdAndCarrierId(id, SecurityUtils.requireCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("Декларацію не знайдено: " + id));
    }

    private CmrDocumentDto toDto(CmrDocument doc) {
        List<CmrExtractedFieldDto> fields = fromJson(doc.getExtractedFieldsJson()).stream()
                .map(f -> new CmrExtractedFieldDto(f.fieldKey(), f.value(), f.confidence()))
                .toList();
        return new CmrDocumentDto(
                doc.getId(),
                doc.getOriginalFilename(),
                doc.getMimeType(),
                doc.getFileSizeBytes(),
                fields,
                doc.getAppliedAt() != null ? doc.getAppliedAt().toString() : null);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Помилка серіалізації JSON", ex);
        }
    }

    private List<ExtractedField> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ExtractedField>>() {});
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }
}
