package com.geosun.rmpd.application.dto;

import com.geosun.rmpd.domain.enums.PartyRole;

public record PartySuggestionDto(
        Long partyId,
        PartyRole partyRole,
        String name,
        String idNumber,
        double matchScore,
        String source) {}
