package com.geosun.rmpd.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.CmrPartySuggestionsDto;
import com.geosun.rmpd.application.dto.PartySuggestionDto;
import com.geosun.rmpd.application.exception.ResourceNotFoundException;
import com.geosun.rmpd.domain.enums.PartyRole;
import com.geosun.rmpd.domain.model.CmrDocument;
import com.geosun.rmpd.domain.model.Party;
import com.geosun.rmpd.infrastructure.ocr.ExtractedField;
import com.geosun.rmpd.infrastructure.persistence.CmrDocumentRepository;
import com.geosun.rmpd.infrastructure.persistence.DeclarationRepository;
import com.geosun.rmpd.infrastructure.persistence.PartyRepository;
import com.geosun.rmpd.infrastructure.security.SecurityUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartySuggestionService {

    private final PartyRepository partyRepository;
    private final CmrDocumentRepository cmrDocumentRepository;
    private final DeclarationRepository declarationRepository;
    private final ObjectMapper objectMapper;

    public PartySuggestionService(
            PartyRepository partyRepository,
            CmrDocumentRepository cmrDocumentRepository,
            DeclarationRepository declarationRepository,
            ObjectMapper objectMapper) {
        this.partyRepository = partyRepository;
        this.cmrDocumentRepository = cmrDocumentRepository;
        this.declarationRepository = declarationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PartySuggestionDto> search(String query, PartyRole role) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Long carrierId = SecurityUtils.requireCarrierId();
        return partyRepository.findByCarrierIdAndNameContainingIgnoreCaseOrderByNameAsc(carrierId, query.trim())
                .stream()
                .filter(p -> role == null || matchesRole(p.getPartyRole(), role))
                .map(p -> toSuggestion(p, score(query, p.getName()), "DIRECTORY"))
                .sorted(Comparator.comparingDouble(PartySuggestionDto::matchScore).reversed())
                .limit(10)
                .toList();
    }

    @Transactional(readOnly = true)
    public CmrPartySuggestionsDto fromCmr(Long declarationId) {
        requireDeclaration(declarationId);
        CmrDocument doc = cmrDocumentRepository.findByDeclarationIdOrderByCreatedAtDesc(declarationId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CMR не знайдено"));
        Map<String, String> fields = fromJson(doc.getExtractedFieldsJson()).stream()
                .collect(Collectors.toMap(ExtractedField::fieldKey, ExtractedField::value, (a, b) -> b));
        String senderName = fields.get("senderName");
        String receiverName = fields.get("receiverName");
        return new CmrPartySuggestionsDto(
                senderName,
                receiverName,
                matchName(senderName, PartyRole.SENDER),
                matchName(receiverName, PartyRole.RECEIVER));
    }

    private PartySuggestionDto matchName(String extractedName, PartyRole role) {
        if (extractedName == null || extractedName.isBlank()) {
            return null;
        }
        Long carrierId = SecurityUtils.requireCarrierId();
        return partyRepository.findByCarrierIdOrderByNameAsc(carrierId).stream()
                .filter(p -> matchesRole(p.getPartyRole(), role))
                .map(p -> toSuggestion(p, score(extractedName, p.getName()), "CMR_MATCH"))
                .filter(s -> s.matchScore() >= 0.5)
                .max(Comparator.comparingDouble(PartySuggestionDto::matchScore))
                .orElse(null);
    }

    private PartySuggestionDto toSuggestion(Party party, double matchScore, String source) {
        return new PartySuggestionDto(
                party.getId(), party.getPartyRole(), party.getName(), party.getIdNumber(), matchScore, source);
    }

    private boolean matchesRole(PartyRole partyRole, PartyRole required) {
        return partyRole == required || partyRole == PartyRole.BOTH;
    }

    private double score(String query, String candidate) {
        if (query == null || candidate == null) {
            return 0;
        }
        String q = query.toLowerCase(Locale.ROOT).trim();
        String c = candidate.toLowerCase(Locale.ROOT).trim();
        if (q.equals(c)) {
            return 1.0;
        }
        if (c.contains(q) || q.contains(c)) {
            return 0.85;
        }
        String[] qTokens = q.split("\\s+");
        long hits = 0;
        for (String token : qTokens) {
            if (token.length() > 2 && c.contains(token)) {
                hits++;
            }
        }
        return qTokens.length == 0 ? 0 : (double) hits / qTokens.length;
    }

    private void requireDeclaration(Long id) {
        declarationRepository.findByIdAndCarrierId(id, SecurityUtils.requireCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("Декларацію не знайдено: " + id));
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
