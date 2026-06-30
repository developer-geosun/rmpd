package com.geosun.rmpd.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.GpsCheckResultDto;
import com.geosun.rmpd.application.dto.SubmitResultDto;
import com.geosun.rmpd.domain.enums.DeclarationEventType;
import com.geosun.rmpd.domain.enums.DeclarationStatus;
import com.geosun.rmpd.domain.model.Declaration;
import com.geosun.rmpd.domain.model.DeclarationEvent;
import com.geosun.rmpd.domain.model.PuescCredential;
import com.geosun.rmpd.domain.model.Vehicle;
import com.geosun.rmpd.infrastructure.gps.GpsPosition;
import com.geosun.rmpd.infrastructure.gps.GpsPositionProvider;
import com.geosun.rmpd.infrastructure.persistence.DeclarationEventRepository;
import com.geosun.rmpd.infrastructure.persistence.DeclarationRepository;
import com.geosun.rmpd.infrastructure.puesc.AcceptDocumentResult;
import com.geosun.rmpd.infrastructure.puesc.PuescSoapClient;
import com.geosun.rmpd.infrastructure.xml.Rmpd406XmlGenerator;
import com.geosun.rmpd.infrastructure.xml.XmlSigningService;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GpsCheckService {

    private final DeclarationService declarationService;
    private final DeclarationRepository declarationRepository;
    private final DeclarationEventRepository eventRepository;
    private final GpsPositionProvider gpsPositionProvider;
    private final PuescCredentialService credentialService;
    private final PuescSoapClient puescSoapClient;
    private final XmlSigningService xmlSigningService;
    private final ObjectMapper objectMapper;
    private final Duration staleThreshold;

    public GpsCheckService(
            DeclarationService declarationService,
            DeclarationRepository declarationRepository,
            DeclarationEventRepository eventRepository,
            GpsPositionProvider gpsPositionProvider,
            PuescCredentialService credentialService,
            PuescSoapClient puescSoapClient,
            XmlSigningService xmlSigningService,
            ObjectMapper objectMapper,
            @Value("${rmpd.gps.stale-threshold-hours:24}") int staleThresholdHours) {
        this.declarationService = declarationService;
        this.declarationRepository = declarationRepository;
        this.eventRepository = eventRepository;
        this.gpsPositionProvider = gpsPositionProvider;
        this.credentialService = credentialService;
        this.puescSoapClient = puescSoapClient;
        this.xmlSigningService = xmlSigningService;
        this.objectMapper = objectMapper;
        this.staleThreshold = Duration.ofHours(Math.max(1, staleThresholdHours));
    }

    @Transactional
    public GpsCheckResultDto check(Long declarationId) {
        Declaration declaration = requireRegistered(declarationId);
        GpsCheckResultDto result = resolveCheck(declaration);
        addEvent(declaration, DeclarationEventType.GPS_CHECK, result);
        return result;
    }

    private GpsCheckResultDto resolveCheck(Declaration declaration) {
        Vehicle vehicle = declaration.getVehicle();
        if (vehicle == null || vehicle.getGpsDeviceId() == null || vehicle.getGpsDeviceId().isBlank()) {
            return new GpsCheckResultDto(
                    false, null, null, null, null, null, true, "GPS-локатор не вказано в декларації");
        }
        String deviceId = vehicle.getGpsDeviceId();
        Optional<GpsPosition> positionOpt = gpsPositionProvider.fetchLastPosition(deviceId);
        if (positionOpt.isEmpty()) {
            return new GpsCheckResultDto(
                    false, deviceId, null, null, null, null, true, "Позицію GPS не отримано від провайдера");
        }
        GpsPosition position = positionOpt.get();
        boolean stale = position.recordedAt().isBefore(Instant.now().minus(staleThreshold));
        boolean valid = !stale;
        String message = valid
                ? "GPS активний, остання позиція свіжа"
                : "Позиція застаріла (старіше " + staleThreshold.toHours() + " год)";
        return new GpsCheckResultDto(
                valid,
                deviceId,
                position.latitude(),
                position.longitude(),
                position.recordedAt(),
                position.source(),
                stale,
                message);
    }

    private GpsPosition toPosition(GpsCheckResultDto check) {
        if (check.latitude() == null) {
            return null;
        }
        return new GpsPosition(
                check.gpsDeviceId(),
                check.latitude(),
                check.longitude(),
                check.recordedAt(),
                check.source());
    }

    @Transactional(readOnly = true)
    public byte[] generateXml(Long declarationId) {
        Declaration declaration = requireRegistered(declarationId);
        GpsCheckResultDto check = resolveCheck(declaration);
        GpsPosition position = toPosition(check);
        return Rmpd406XmlGenerator.generate(declaration, position).getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public SubmitResultDto submitCheck(Long declarationId) {
        Declaration declaration = requireRegistered(declarationId);
        GpsCheckResultDto check = check(declarationId);
        if (!check.valid()) {
            throw new IllegalStateException("GPS-перевірка не пройдена: " + check.message());
        }
        PuescCredential credential = credentialService.requireActiveCredential();
        String password = credentialService.decryptPassword(credential);
        byte[] xml = generateXml(declarationId);
        byte[] signed = xmlSigningService.sign(xml, credential.getSigningCertPath());
        AcceptDocumentResult result = puescSoapClient.acceptDocument(
                credential.getUsername(),
                password,
                signed,
                "rmpd406-" + declarationId + ".xml");
        declaration.setPuescSysRef(result.sysRef());
        declarationRepository.save(declaration);
        addEvent(declaration, DeclarationEventType.GPS_CHECK_SUBMITTED, check);
        return new SubmitResultDto(result.sysRef(), declaration.getStatus().name(), "RMPD406 відправлено в PUESC");
    }

    private Declaration requireRegistered(Long id) {
        Declaration declaration = declarationService.requireDeclaration(id);
        if (declaration.getStatus() != DeclarationStatus.REGISTERED) {
            throw new IllegalStateException("GPS-перевірка доступна лише для зареєстрованих декларацій");
        }
        if (declaration.getReferenceNumber() == null || declaration.getReferenceNumber().isBlank()) {
            throw new IllegalStateException("Відсутній референсний номер SENT");
        }
        return declaration;
    }

    private void addEvent(Declaration declaration, DeclarationEventType type, GpsCheckResultDto payload) {
        DeclarationEvent event = new DeclarationEvent();
        event.setDeclaration(declaration);
        event.setEventType(type);
        try {
            event.setPayloadJson(objectMapper.writeValueAsString(Map.of(
                    "valid", payload.valid(),
                    "gpsDeviceId", payload.gpsDeviceId() != null ? payload.gpsDeviceId() : "",
                    "latitude", payload.latitude() != null ? payload.latitude() : "",
                    "longitude", payload.longitude() != null ? payload.longitude() : "",
                    "message", payload.message() != null ? payload.message() : "")));
        } catch (Exception ex) {
            event.setPayloadJson("{}");
        }
        eventRepository.save(event);
    }
}
