package com.geosun.rmpd.application.service;

import com.geosun.rmpd.domain.model.AuditLog;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.User;
import com.geosun.rmpd.infrastructure.persistence.AuditLogRepository;
import com.geosun.rmpd.infrastructure.persistence.CarrierRepository;
import com.geosun.rmpd.infrastructure.persistence.UserRepository;
import com.geosun.rmpd.infrastructure.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final CarrierRepository carrierRepository;
    private final UserRepository userRepository;

    public AuditService(
            AuditLogRepository auditLogRepository,
            CarrierRepository carrierRepository,
            UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.carrierRepository = carrierRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void log(String action, String resourceType, Long resourceId, String detailsJson, String ipAddress) {
        Long carrierId = SecurityUtils.requireCarrierId();
        Carrier carrier = carrierRepository.findById(carrierId).orElseThrow();
        AuditLog entry = new AuditLog();
        entry.setCarrier(carrier);
        Long userId = SecurityUtils.requireCurrentUser().userId();
        User user = userRepository.findById(userId).orElse(null);
        entry.setUser(user);
        entry.setAction(action);
        entry.setResourceType(resourceType);
        entry.setResourceId(resourceId);
        entry.setDetailsJson(detailsJson);
        entry.setIpAddress(ipAddress);
        auditLogRepository.save(entry);
    }
}
