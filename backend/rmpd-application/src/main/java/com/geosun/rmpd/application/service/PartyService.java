package com.geosun.rmpd.application.service;

import com.geosun.rmpd.application.dto.PartyDto;
import com.geosun.rmpd.application.dto.PartyUpsertDto;
import com.geosun.rmpd.application.exception.ResourceNotFoundException;
import com.geosun.rmpd.application.support.JsonMapper;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.Party;
import com.geosun.rmpd.infrastructure.persistence.CarrierRepository;
import com.geosun.rmpd.infrastructure.persistence.PartyRepository;
import com.geosun.rmpd.infrastructure.security.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartyService {

    private final PartyRepository partyRepository;
    private final CarrierRepository carrierRepository;
    private final JsonMapper jsonMapper;

    public PartyService(
            PartyRepository partyRepository,
            CarrierRepository carrierRepository,
            JsonMapper jsonMapper) {
        this.partyRepository = partyRepository;
        this.carrierRepository = carrierRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(readOnly = true)
    public List<PartyDto> list() {
        return partyRepository.findByCarrierIdOrderByNameAsc(SecurityUtils.requireCarrierId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PartyDto get(Long id) {
        return toDto(requireParty(id));
    }

    @Transactional
    public PartyDto create(PartyUpsertDto dto) {
        Party party = new Party();
        party.setCarrier(requireCarrier());
        apply(party, dto);
        return toDto(partyRepository.save(party));
    }

    @Transactional
    public PartyDto update(Long id, PartyUpsertDto dto) {
        Party party = requireParty(id);
        apply(party, dto);
        return toDto(partyRepository.save(party));
    }

    @Transactional
    public void delete(Long id) {
        partyRepository.delete(requireParty(id));
    }

    private Party requireParty(Long id) {
        return partyRepository.findByIdAndCarrierId(id, SecurityUtils.requireCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("Контрагента не знайдено: " + id));
    }

    private Carrier requireCarrier() {
        return carrierRepository.findById(SecurityUtils.requireCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("Перевізника не знайдено"));
    }

    private void apply(Party party, PartyUpsertDto dto) {
        party.setPartyRole(dto.partyRole());
        party.setIdType(dto.idType());
        party.setIdNumber(dto.idNumber());
        party.setName(dto.name());
        party.setAddressJson(jsonMapper.toJson(dto.address()));
    }

    private PartyDto toDto(Party party) {
        return new PartyDto(
                party.getId(),
                party.getPartyRole(),
                party.getIdType(),
                party.getIdNumber(),
                party.getName(),
                jsonMapper.toAddress(party.getAddressJson()),
                party.getCreatedAt());
    }
}
