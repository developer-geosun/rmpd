package com.geosun.rmpd.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.domain.enums.DeclarationEventType;
import com.geosun.rmpd.domain.enums.DeclarationStatus;
import com.geosun.rmpd.domain.model.Declaration;
import com.geosun.rmpd.domain.model.DeclarationEvent;
import com.geosun.rmpd.domain.model.PuescCredential;
import com.geosun.rmpd.infrastructure.persistence.DeclarationEventRepository;
import com.geosun.rmpd.infrastructure.persistence.DeclarationRepository;
import com.geosun.rmpd.infrastructure.puesc.PuescIncomingDocument;
import com.geosun.rmpd.infrastructure.puesc.PuescSoapClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PuescPollingService {

    private static final Logger log = LoggerFactory.getLogger(PuescPollingService.class);

    private final DeclarationRepository declarationRepository;
    private final DeclarationEventRepository eventRepository;
    private final PuescCredentialService credentialService;
    private final PuescSoapClient puescSoapClient;
    private final RegistrationEmailService emailService;
    private final ObjectMapper objectMapper;
    private final String targetSystem;

    public PuescPollingService(
            DeclarationRepository declarationRepository,
            DeclarationEventRepository eventRepository,
            PuescCredentialService credentialService,
            PuescSoapClient puescSoapClient,
            RegistrationEmailService emailService,
            ObjectMapper objectMapper,
            @Value("${puesc.target-system:SENT}") String targetSystem) {
        this.declarationRepository = declarationRepository;
        this.eventRepository = eventRepository;
        this.credentialService = credentialService;
        this.puescSoapClient = puescSoapClient;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
        this.targetSystem = targetSystem;
    }

    @Transactional
    public void pollSubmittedDeclarations() {
        List<Declaration> pending = declarationRepository.findByStatusAndPuescSysRefNotNull(DeclarationStatus.SUBMITTED);
        if (pending.isEmpty()) {
            return;
        }
        try {
            PuescCredential credential = credentialService.requireActiveCredential();
            String password = credentialService.decryptPassword(credential);
            PuescIncomingDocument incoming = puescSoapClient.getNextDocument(
                    credential.getUsername(), password, targetSystem);
            if (incoming == null) {
                return;
            }
            Optional<Declaration> match = declarationRepository.findByPuescSysRef(incoming.sysRef());
            if (match.isEmpty()) {
                log.warn("Отримано документ SEAP без відповідної декларації: sysRef={}", incoming.sysRef());
                return;
            }
            processResponse(match.get(), incoming);
        } catch (Exception ex) {
            log.warn("PUESC polling failed: {}", ex.getMessage());
        }
    }

    private void processResponse(Declaration declaration, PuescIncomingDocument incoming) {
        addEvent(declaration, DeclarationEventType.UPP_RECEIVED, payload(incoming));
        addEvent(declaration, DeclarationEventType.UPO_RECEIVED, payload(incoming));

        if (incoming.rejected()) {
            declaration.setStatus(DeclarationStatus.REJECTED);
            addEvent(declaration, DeclarationEventType.REJECTED, payload(incoming));
        } else {
            declaration.setReferenceNumber(incoming.referenceNumber());
            declaration.setStatus(DeclarationStatus.REGISTERED);
            addEvent(declaration, DeclarationEventType.REGISTERED, payload(incoming));
            emailService.sendRegistrationNotification(declaration);
        }
        declarationRepository.save(declaration);
        log.info("Declaration {} status={} ref={}", declaration.getId(), declaration.getStatus(), declaration.getReferenceNumber());
    }

    private void addEvent(Declaration declaration, DeclarationEventType type, String payload) {
        DeclarationEvent event = new DeclarationEvent();
        event.setDeclaration(declaration);
        event.setEventType(type);
        event.setPayloadJson(payload);
        eventRepository.save(event);
    }

    private String payload(PuescIncomingDocument doc) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "sysRef", doc.sysRef() != null ? doc.sysRef() : "",
                    "referenceNumber", doc.referenceNumber() != null ? doc.referenceNumber() : "",
                    "documentType", doc.documentType() != null ? doc.documentType() : ""));
        } catch (Exception ex) {
            return "{}";
        }
    }
}
