package com.geosun.rmpd.application.service;

import com.geosun.rmpd.application.dto.AuditLogDto;
import com.geosun.rmpd.domain.model.AuditLog;
import com.geosun.rmpd.infrastructure.persistence.AuditLogRepository;
import com.geosun.rmpd.infrastructure.security.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;

    public AuditQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> listForCarrier() {
        return auditLogRepository.findByCarrierIdOrderByCreatedAtDesc(SecurityUtils.requireCarrierId()).stream()
                .map(this::toDto)
                .toList();
    }

    private AuditLogDto toDto(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getUser() != null ? log.getUser().getId() : null,
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getDetailsJson(),
                log.getIpAddress(),
                log.getCreatedAt());
    }
}
