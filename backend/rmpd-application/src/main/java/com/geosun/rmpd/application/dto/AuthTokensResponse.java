package com.geosun.rmpd.application.dto;

import com.geosun.rmpd.domain.enums.UserRole;

public record AuthTokensResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserInfoResponse user
) {
}
