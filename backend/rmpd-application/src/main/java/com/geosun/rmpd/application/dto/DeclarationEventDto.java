package com.geosun.rmpd.application.dto;

import com.geosun.rmpd.domain.enums.DeclarationEventType;
import java.time.Instant;

public record DeclarationEventDto(
        Long id,
        DeclarationEventType eventType,
        String payloadJson,
        Instant createdAt) {}
