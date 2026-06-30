package com.geosun.rmpd.application.dto;

import com.geosun.rmpd.domain.enums.UserRole;

public record UserInfoResponse(Long id, String email, UserRole role, Long carrierId) {
}
