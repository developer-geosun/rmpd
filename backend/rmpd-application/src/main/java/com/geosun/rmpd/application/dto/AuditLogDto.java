package com.geosun.rmpd.application.dto;

import java.time.Instant;

public record AuditLogDto(
        Long id,
        Long userId,
        String action,
        String resourceType,
        Long resourceId,
        String detailsJson,
        String ipAddress,
        Instant createdAt) {}
