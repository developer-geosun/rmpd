package com.geosun.rmpd.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.AmendmentRequestDto;
import com.geosun.rmpd.application.dto.DeclarationDto;
import com.geosun.rmpd.application.dto.SubmitResultDto;
import com.geosun.rmpd.application.exception.ResourceNotFoundException;
import com.geosun.rmpd.application.validation.LatinInputValidator;
import com.geosun.rmpd.domain.enums.DeclarationEventType;
import com.geosun.rmpd.domain.enums.DeclarationStatus;
import com.geosun.rmpd.domain.model.Declaration;
import com.geosun.rmpd.domain.model.DeclarationEvent;
import com.geosun.rmpd.domain.model.Vehicle;
import com.geosun.rmpd.infrastructure.persistence.DeclarationEventRepository;
import com.geosun.rmpd.infrastructure.persistence.DeclarationRepository;
import com.geosun.rmpd.infrastructure.persistence.VehicleRepository;
import com.geosun.rmpd.infrastructure.puesc.AcceptDocumentResult;
import com.geosun.rmpd.infrastructure.puesc.PuescSoapClient;
import com.geosun.rmpd.infrastructure.security.SecurityUtils;
import com.geosun.rmpd.infrastructure.xml.RmpdAmendmentXmlGenerator;
import com.geosun.rmpd.infrastructure.xml.XmlSigningService;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeclarationAmendmentService {

    private final DeclarationRepository declarationRepository;
    private final DeclarationEventRepository eventRepository;
    private final VehicleRepository vehicleRepository;
    private final DeclarationService declarationService;
    private final PuescCredentialService credentialService;
    private final PuescSoapClient puescSoapClient;
    private final XmlSigningService xmlSigningService;
    private final ObjectMapper objectMapper;

    public DeclarationAmendmentService(
            DeclarationRepository declarationRepository,
            DeclarationEventRepository eventRepository,
            VehicleRepository vehicleRepository,
            DeclarationService declarationService,
            PuescCredentialService credentialService,
            PuescSoapClient puescSoapClient,
            XmlSigningService xmlSigningService,
            ObjectMapper objectMapper) {
        this.declarationRepository = declarationRepository;
        this.eventRepository = eventRepository;
        this.vehicleRepository = vehicleRepository;
        this.declarationService = declarationService;
        this.credentialService = credentialService;
        this.puescSoapClient = puescSoapClient;
        this.xmlSigningService = xmlSigningService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DeclarationDto applyAmendment(Long id, AmendmentRequestDto dto) {
        Declaration declaration = requireRegistered(id);
        if (dto.comment() != null) {
            LatinInputValidator.requireLatin(dto.comment(), "comment");
            declaration.setComment(dto.comment());
        }
        if (dto.vehicleId() != null) {
            declaration.setVehicle(resolveVehicle(dto.vehicleId()));
        }
        if (dto.routeStartDate() != null) {
            declaration.setRouteStartDate(dto.routeStartDate());
        }
        if (dto.routeEndDate() != null) {
            declaration.setRouteEndDate(dto.routeEndDate());
        }
        declaration = declarationRepository.save(declaration);
        addEvent(declaration, DeclarationEventType.AMENDMENT_APPLIED, dto.amendmentReason());
        return declarationService.get(id);
    }

    @Transactional(readOnly = true)
    public byte[] generateAmendmentXml(Long id) {
        Declaration declaration = requireRegistered(id);
        String reason = resolveLastAmendmentReason(id);
        return RmpdAmendmentXmlGenerator.generate(declaration, reason).getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public SubmitResultDto submitAmendment(Long id, AmendmentRequestDto dto) {
        if (dto.amendmentReason() == null || dto.amendmentReason().isBlank()) {
            throw new IllegalArgumentException("Потрібна причина актуалізації (amendmentReason)");
        }
        applyAmendment(id, dto);
        Declaration declaration = requireRegistered(id);
        var credential = credentialService.requireActiveCredential();
        String password = credentialService.decryptPassword(credential);

        byte[] xml = RmpdAmendmentXmlGenerator.generate(declaration, dto.amendmentReason())
                .getBytes(StandardCharsets.UTF_8);
        byte[] signed = xmlSigningService.sign(xml, credential.getSigningCertPath());

        AcceptDocumentResult result = puescSoapClient.acceptDocument(
                credential.getUsername(),
                password,
                signed,
                "rmpd-amend-" + id + ".xml");

        declaration.setPuescSysRef(result.sysRef());
        declarationRepository.save(declaration);
        addEvent(declaration, DeclarationEventType.AMENDMENT_SUBMITTED, dto.amendmentReason());

        return new SubmitResultDto(result.sysRef(), declaration.getStatus().name(), "Актуалізацію відправлено в PUESC");
    }

    private Declaration requireRegistered(Long id) {
        Declaration declaration = declarationService.requireDeclaration(id);
        if (declaration.getStatus() != DeclarationStatus.REGISTERED) {
            throw new IllegalStateException("Актуалізація доступна лише для зареєстрованих декларацій");
        }
        if (declaration.getReferenceNumber() == null || declaration.getReferenceNumber().isBlank()) {
            throw new IllegalStateException("Відсутній референсний номер SENT");
        }
        return declaration;
    }

    private Vehicle resolveVehicle(Long vehicleId) {
        return vehicleRepository.findByIdAndCarrierId(vehicleId, SecurityUtils.requireCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("ТЗ не знайдено"));
    }

    private String resolveLastAmendmentReason(Long declarationId) {
        return eventRepository.findByDeclarationIdOrderByCreatedAtAsc(declarationId).stream()
                .filter(e -> e.getEventType() == DeclarationEventType.AMENDMENT_APPLIED)
                .reduce((first, second) -> second)
                .map(DeclarationEvent::getPayloadJson)
                .orElse("");
    }

    private void addEvent(Declaration declaration, DeclarationEventType type, String reason) {
        DeclarationEvent event = new DeclarationEvent();
        event.setDeclaration(declaration);
        event.setEventType(type);
        try {
            event.setPayloadJson(objectMapper.writeValueAsString(Map.of("amendmentReason", reason != null ? reason : "")));
        } catch (Exception ex) {
            event.setPayloadJson("{}");
        }
        eventRepository.save(event);
    }
}
