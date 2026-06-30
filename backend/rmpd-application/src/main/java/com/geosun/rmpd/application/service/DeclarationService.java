package com.geosun.rmpd.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.DeclarationDto;
import com.geosun.rmpd.application.dto.DeclarationEventDto;
import com.geosun.rmpd.application.dto.DeclarationUpsertDto;
import com.geosun.rmpd.application.dto.ValidationResultDto;
import com.geosun.rmpd.application.exception.ResourceNotFoundException;
import com.geosun.rmpd.application.validation.LatinInputValidator;
import com.geosun.rmpd.domain.enums.DeclarationEventType;
import com.geosun.rmpd.domain.enums.DeclarationStatus;
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

    public DeclarationService(
            DeclarationRepository declarationRepository,
            DeclarationEventRepository eventRepository,
            CarrierRepository carrierRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            PermitRepository permitRepository,
            PartyRepository partyRepository,
            XsdValidator xsdValidator,
            ObjectMapper objectMapper) {
        this.declarationRepository = declarationRepository;
        this.eventRepository = eventRepository;
        this.carrierRepository = carrierRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.permitRepository = permitRepository;
        this.partyRepository = partyRepository;
        this.xsdValidator = xsdValidator;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<DeclarationDto> list() {
        return declarationRepository.findByCarrierIdOrderByUpdatedAtDesc(SecurityUtils.requireCarrierId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeclarationDto get(Long id) {
        return toDto(requireDeclaration(id));
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
        declaration.setTransportType(dto.transportType());
        declaration.setCmrNumber(dto.cmrNumber());
        declaration.setRouteStartDate(dto.routeStartDate());
        declaration.setRouteEndDate(dto.routeEndDate());
        declaration.setLoadingCountry(upper(dto.loadingCountry()));
        declaration.setUnloadingCountry(upper(dto.unloadingCountry()));
        declaration.setVehicle(resolveVehicle(dto.vehicleId()));
        declaration.setPermit(resolvePermit(dto.permitId()));
        declaration.setSenderParty(resolveParty(dto.senderPartyId()));
        declaration.setReceiverParty(resolveParty(dto.receiverPartyId()));
        declaration.setRoutePointsJson(dto.routePointsJson());
        declaration.setComment(dto.comment());
    }

    private List<String> validateBusiness(Declaration d) {
        List<String> errors = new ArrayList<>();
        if (d.getVehicle() == null) {
            errors.add("Не обрано транспортний засіб");
        }
        if (d.getRouteStartDate() == null) {
            errors.add("Не вказано дату початку перевезення");
        }
        if (d.getLoadingCountry() == null || d.getUnloadingCountry() == null) {
            errors.add("Не вказано країни завантаження/розвантаження");
        }
        if (d.getCmrNumber() != null && !LatinInputValidator.isLatin(d.getCmrNumber())) {
            errors.add("Номер CMR має бути латиницею");
        }
        return errors;
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
                d.getCreatedAt(),
                d.getUpdatedAt());
    }
}
