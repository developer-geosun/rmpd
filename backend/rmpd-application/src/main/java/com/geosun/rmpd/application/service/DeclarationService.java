package com.geosun.rmpd.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.DeclarationDto;
import com.geosun.rmpd.application.dto.DeclarationEventDto;
import com.geosun.rmpd.application.dto.DeclarationProgressDto;
import com.geosun.rmpd.application.dto.DeclarationUpsertDto;
import com.geosun.rmpd.application.dto.RoutePointDto;
import com.geosun.rmpd.application.dto.ValidationResultDto;
import com.geosun.rmpd.application.exception.ResourceNotFoundException;
import com.geosun.rmpd.application.validation.LatinInputValidator;
import com.geosun.rmpd.domain.enums.DeclarationEventType;
import com.geosun.rmpd.domain.enums.DeclarationStatus;
import com.geosun.rmpd.domain.enums.TransportType;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.Declaration;
import com.geosun.rmpd.domain.model.DeclarationEvent;
import com.geosun.rmpd.domain.model.Party;
import com.geosun.rmpd.domain.model.Permit;
import com.geosun.rmpd.domain.model.User;
import com.geosun.rmpd.domain.model.Vehicle;
import com.geosun.rmpd.infrastructure.persistence.CarrierRepository;
import com.geosun.rmpd.infrastructure.persistence.DeclarationEventRepository;
import com.geosun.rmpd.infrastructure.persistence.DeclarationRepository;
import com.geosun.rmpd.infrastructure.persistence.PartyRepository;
import com.geosun.rmpd.infrastructure.persistence.PermitRepository;
import com.geosun.rmpd.infrastructure.persistence.UserRepository;
import com.geosun.rmpd.infrastructure.persistence.VehicleRepository;
import com.geosun.rmpd.infrastructure.security.SecurityUtils;
import com.geosun.rmpd.infrastructure.xml.RmpdXmlGenerator;
import com.geosun.rmpd.infrastructure.xml.XsdValidator;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeclarationService {

    private final DeclarationRepository declarationRepository;
    private final DeclarationEventRepository eventRepository;
    private final CarrierRepository carrierRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final PermitRepository permitRepository;
    private final PartyRepository partyRepository;
    private final XsdValidator xsdValidator;
    private final ObjectMapper objectMapper;
    private final DeclarationCompletionService completionService;

    public DeclarationService(
            DeclarationRepository declarationRepository,
            DeclarationEventRepository eventRepository,
            CarrierRepository carrierRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            PermitRepository permitRepository,
            PartyRepository partyRepository,
            XsdValidator xsdValidator,
            ObjectMapper objectMapper,
            DeclarationCompletionService completionService) {
        this.declarationRepository = declarationRepository;
        this.eventRepository = eventRepository;
        this.carrierRepository = carrierRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.permitRepository = permitRepository;
        this.partyRepository = partyRepository;
        this.xsdValidator = xsdValidator;
        this.objectMapper = objectMapper;
        this.completionService = completionService;
    }

    @Transactional(readOnly = true)
    public List<DeclarationDto> list(DeclarationStatus status) {
        Long carrierId = SecurityUtils.requireCarrierId();
        List<Declaration> declarations = status == null
                ? declarationRepository.findByCarrierIdOrderByUpdatedAtDesc(carrierId)
                : declarationRepository.findByCarrierIdAndStatusOrderByUpdatedAtDesc(carrierId, status);
        return declarations.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DeclarationDto get(Long id) {
        return toDto(requireDeclaration(id));
    }

    @Transactional(readOnly = true)
    public DeclarationProgressDto progress(Long id) {
        return completionService.evaluate(requireDeclaration(id));
    }

    @Transactional
    public DeclarationDto create() {
        Long carrierId = SecurityUtils.requireCarrierId();
        Long userId = SecurityUtils.requireCurrentUser().userId();
        Carrier carrier = carrierRepository.findById(carrierId)
                .orElseThrow(() -> new ResourceNotFoundException("Перевізника не знайдено"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено"));

        Declaration declaration = new Declaration();
        declaration.setCarrier(carrier);
        declaration.setCreatedBy(user);
        declaration.setStatus(DeclarationStatus.DRAFT);
        declaration = declarationRepository.save(declaration);

        addEvent(declaration, DeclarationEventType.CREATED, null);
        return toDto(declaration);
    }

    @Transactional
    public DeclarationDto update(Long id, DeclarationUpsertDto dto) {
        Declaration declaration = requireEditable(id);
        apply(declaration, dto);
        declaration = declarationRepository.save(declaration);
        return toDto(declaration);
    }

    @Transactional
    public DeclarationDto copy(Long id) {
        Declaration source = requireDeclaration(id);
        Declaration copy = new Declaration();
        copy.setCarrier(source.getCarrier());
        copy.setCreatedBy(source.getCreatedBy());
        copy.setStatus(DeclarationStatus.DRAFT);
        copy.setTransportType(source.getTransportType());
        copy.setCmrNumber(source.getCmrNumber());
        copy.setRouteStartDate(source.getRouteStartDate());
        copy.setRouteEndDate(source.getRouteEndDate());
        copy.setLoadingCountry(source.getLoadingCountry());
        copy.setUnloadingCountry(source.getUnloadingCountry());
        copy.setVehicle(source.getVehicle());
        copy.setPermit(source.getPermit());
        copy.setSenderParty(source.getSenderParty());
        copy.setReceiverParty(source.getReceiverParty());
        copy.setRoutePointsJson(source.getRoutePointsJson());
        copy.setComment(source.getComment());
        copy.setTermsAccepted(false);
        copy = declarationRepository.save(copy);
        addEvent(copy, DeclarationEventType.CREATED, "{\"copiedFrom\":" + id + "}");
        return toDto(copy);
    }

    @Transactional(readOnly = true)
    public List<DeclarationEventDto> listEvents(Long id) {
        requireDeclaration(id);
        return eventRepository.findByDeclarationIdOrderByCreatedAtAsc(id).stream()
                .map(e -> new DeclarationEventDto(e.getId(), e.getEventType(), e.getPayloadJson(), e.getCreatedAt()))
                .toList();
    }

    @Transactional
    public ValidationResultDto validate(Long id) {
        Declaration declaration = requireDeclaration(id);
        List<String> errors = validateBusiness(declaration);
        if (errors.isEmpty() && xsdValidator.isXsdAvailable()) {
            try {
                String xml = RmpdXmlGenerator.generate(declaration);
                errors.addAll(xsdValidator.validate(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
            } catch (Exception ex) {
                errors.add("XSD: " + ex.getMessage());
            }
        }
        boolean valid = errors.isEmpty();
        if (valid) {
            declaration.setStatus(DeclarationStatus.VALIDATED);
            declarationRepository.save(declaration);
            addEvent(declaration, DeclarationEventType.VALIDATED, null);
        }
        return new ValidationResultDto(valid, errors);
    }

    @Transactional(readOnly = true)
    public byte[] generateXml(Long id) {
        Declaration declaration = requireDeclaration(id);
        return RmpdXmlGenerator.generate(declaration).getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public Declaration requireDeclaration(Long id) {
        return declarationRepository.findByIdAndCarrierId(id, SecurityUtils.requireCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("Декларацію не знайдено: " + id));
    }

    private Declaration requireEditable(Long id) {
        Declaration declaration = requireDeclaration(id);
        if (declaration.getStatus() != DeclarationStatus.DRAFT
                && declaration.getStatus() != DeclarationStatus.VALIDATED) {
            throw new IllegalStateException("Декларацію не можна редагувати у статусі " + declaration.getStatus());
        }
        return declaration;
    }

    private void apply(Declaration declaration, DeclarationUpsertDto dto) {
        if (dto.cmrNumber() != null) {
            LatinInputValidator.requireLatin(dto.cmrNumber(), "cmrNumber");
        }
        if (dto.comment() != null) {
            LatinInputValidator.requireLatin(dto.comment(), "comment");
        }
        TransportType transportType = dto.transportType();
        declaration.setTransportType(transportType);
        declaration.setCmrNumber(dto.cmrNumber());
        declaration.setRouteStartDate(dto.routeStartDate());
        declaration.setRouteEndDate(dto.routeEndDate());
        declaration.setLoadingCountry(upper(dto.loadingCountry()));
        declaration.setUnloadingCountry(upper(dto.unloadingCountry()));
        declaration.setVehicle(resolveVehicle(dto.vehicleId()));
        declaration.setPermit(resolvePermit(dto.permitId()));
        if (completionService.requiresParties(transportType)) {
            declaration.setSenderParty(resolveParty(dto.senderPartyId()));
            declaration.setReceiverParty(resolveParty(dto.receiverPartyId()));
        } else {
            declaration.setSenderParty(null);
            declaration.setReceiverParty(null);
        }
        declaration.setRoutePointsJson(dto.routePointsJson());
        declaration.setComment(dto.comment());
        if (dto.termsAccepted() != null) {
            declaration.setTermsAccepted(dto.termsAccepted());
        }
    }

    private List<String> validateBusiness(Declaration d) {
        List<String> errors = new ArrayList<>();
        Carrier carrier = d.getCarrier();
        if (carrier.getIdType() == null || carrier.getIdType().isBlank()) {
            errors.add("Профіль перевізника: не вказано тип ID");
        }
        if (carrier.getIdNumber() == null || carrier.getIdNumber().isBlank()) {
            errors.add("Профіль перевізника: не вказано номер ID");
        }
        if (carrier.getName() == null || carrier.getName().isBlank()) {
            errors.add("Профіль перевізника: не вказано назву");
        }
        if (carrier.getEmail() == null || carrier.getEmail().isBlank()) {
            errors.add("Профіль перевізника: не вказано email");
        }
        if (d.getTransportType() == null) {
            errors.add("Не обрано тип перевезення");
        }
        if (d.getVehicle() == null) {
            errors.add("Не обрано транспортний засіб");
        }
        TransportType type = d.getTransportType();
        if (type == TransportType.LADEN || type == TransportType.CABOTAGE) {
            if (d.getPermit() == null) {
                errors.add("Не обрано дозвіл");
            }
            if (d.getSenderParty() == null) {
                errors.add("Не обрано відправника");
            }
            if (d.getReceiverParty() == null) {
                errors.add("Не обрано отримувача");
            }
        }
        if (d.getRouteStartDate() == null) {
            errors.add("Не вказано дату початку перевезення");
        }
        if (d.getRouteEndDate() == null) {
            errors.add("Не вказано дату завершення перевезення");
        }
        if (d.getRouteStartDate() != null
                && d.getRouteEndDate() != null
                && d.getRouteEndDate().isBefore(d.getRouteStartDate())) {
            errors.add("Дата завершення не може бути раніше дати початку");
        }
        if (d.getLoadingCountry() == null || d.getUnloadingCountry() == null) {
            errors.add("Не вказано країни завантаження/розвантаження");
        }
        if (parseRoutePoints(d.getRoutePointsJson()).isEmpty()) {
            errors.add("Додайте хоча б одну точку маршруту в Польщі");
        }
        if (d.getCmrNumber() != null && !LatinInputValidator.isLatin(d.getCmrNumber())) {
            errors.add("Номер CMR має бути латиницею");
        }
        if (!d.isTermsAccepted()) {
            errors.add("Потрібно підтвердити заяви (Oświadczenia)");
        }
        return errors;
    }

    private List<RoutePointDto> parseRoutePoints(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<RoutePointDto>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Vehicle resolveVehicle(Long id) {
        if (id == null) {
            return null;
        }
        return vehicleRepository.findByIdAndCarrierId(id, SecurityUtils.requireCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("ТЗ не знайдено"));
    }

    private Permit resolvePermit(Long id) {
        if (id == null) {
            return null;
        }
        return permitRepository.findByIdAndCarrierId(id, SecurityUtils.requireCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("Дозвіл не знайдено"));
    }

    private Party resolveParty(Long id) {
        if (id == null) {
            return null;
        }
        return partyRepository.findByIdAndCarrierId(id, SecurityUtils.requireCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("Контрагента не знайдено"));
    }

    private String upper(String value) {
        return value != null ? value.toUpperCase() : null;
    }

    private void addEvent(Declaration declaration, DeclarationEventType type, String payload) {
        DeclarationEvent event = new DeclarationEvent();
        event.setDeclaration(declaration);
        event.setEventType(type);
        event.setPayloadJson(payload);
        eventRepository.save(event);
    }

    private DeclarationDto toDto(Declaration d) {
        int completionPercent = completionService.evaluate(d).completionPercent();
        return new DeclarationDto(
                d.getId(),
                d.getStatus(),
                d.getTransportType(),
                d.getCmrNumber(),
                d.getRouteStartDate(),
                d.getRouteEndDate(),
                d.getLoadingCountry(),
                d.getUnloadingCountry(),
                d.getVehicle() != null ? d.getVehicle().getId() : null,
                d.getPermit() != null ? d.getPermit().getId() : null,
                d.getSenderParty() != null ? d.getSenderParty().getId() : null,
                d.getReceiverParty() != null ? d.getReceiverParty().getId() : null,
                d.getRoutePointsJson(),
                d.getPuescSysRef(),
                d.getReferenceNumber(),
                d.getComment(),
                d.isTermsAccepted(),
                completionPercent,
                d.getCreatedAt(),
                d.getUpdatedAt());
    }
}
