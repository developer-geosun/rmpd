package com.geosun.rmpd.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.SubmitResultDto;
import com.geosun.rmpd.application.dto.ValidationResultDto;
import com.geosun.rmpd.domain.enums.DeclarationEventType;
import com.geosun.rmpd.domain.enums.DeclarationStatus;
import com.geosun.rmpd.domain.model.Declaration;
import com.geosun.rmpd.domain.model.DeclarationEvent;
import com.geosun.rmpd.domain.model.PuescCredential;
import com.geosun.rmpd.infrastructure.persistence.DeclarationEventRepository;
import com.geosun.rmpd.infrastructure.persistence.DeclarationRepository;
import com.geosun.rmpd.infrastructure.puesc.AcceptDocumentResult;
import com.geosun.rmpd.infrastructure.puesc.PuescSoapClient;
import com.geosun.rmpd.infrastructure.xml.XmlSigningService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeclarationSubmitService {

    private static final Logger log = LoggerFactory.getLogger(DeclarationSubmitService.class);

    private final DeclarationService declarationService;
    private final DeclarationRepository declarationRepository;
    private final DeclarationEventRepository eventRepository;
    private final PuescCredentialService credentialService;
    private final PuescSoapClient puescSoapClient;
    private final XmlSigningService xmlSigningService;
    private final ObjectMapper objectMapper;

    public DeclarationSubmitService(
            DeclarationService declarationService,
            DeclarationRepository declarationRepository,
            DeclarationEventRepository eventRepository,
            PuescCredentialService credentialService,
            PuescSoapClient puescSoapClient,
            XmlSigningService xmlSigningService,
            ObjectMapper objectMapper) {
        this.declarationService = declarationService;
        this.declarationRepository = declarationRepository;
        this.eventRepository = eventRepository;
        this.credentialService = credentialService;
        this.puescSoapClient = puescSoapClient;
        this.xmlSigningService = xmlSigningService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SubmitResultDto submit(Long declarationId) {
        Declaration declaration = declarationService.requireDeclaration(declarationId);
        if (declaration.getStatus() == DeclarationStatus.SUBMITTED
                || declaration.getStatus() == DeclarationStatus.REGISTERED) {
            throw new IllegalStateException("Декларація вже відправлена");
        }

        ValidationResultDto validation = declarationService.validate(declarationId);
        if (!validation.valid()) {
            throw new IllegalStateException("Декларація не пройшла валідацію: " + String.join("; ", validation.errors()));
        }

        PuescCredential credential = credentialService.requireActiveCredential();
        String password = credentialService.decryptPassword(credential);

        byte[] xml = declarationService.generateXml(declarationId);
        byte[] signedXml = xmlSigningService.sign(xml, credential.getSigningCertPath());

        declaration.setStatus(DeclarationStatus.SIGNED);
        addEvent(declaration, DeclarationEventType.SIGNED, null);

        AcceptDocumentResult result = puescSoapClient.acceptDocument(
                credential.getUsername(),
                password,
                signedXml,
                "rmpd100-" + declarationId + ".xml");

        declaration.setPuescSysRef(result.sysRef());
        declaration.setStatus(DeclarationStatus.SUBMITTED);
        declarationRepository.save(declaration);

        String payload = toJson(Map.of("sysRef", result.sysRef(), "mock", result.mock()));
        addEvent(declaration, DeclarationEventType.SUBMITTED, payload);
        log.info("Declaration {} submitted sysRef={} mock={}", declarationId, result.sysRef(), result.mock());

        return new SubmitResultDto(result.sysRef(), DeclarationStatus.SUBMITTED.name(), "Відправлено в PUESC");
    }

    private void addEvent(Declaration declaration, DeclarationEventType type, String payload) {
        DeclarationEvent event = new DeclarationEvent();
        event.setDeclaration(declaration);
        event.setEventType(type);
        event.setPayloadJson(payload);
        eventRepository.save(event);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
