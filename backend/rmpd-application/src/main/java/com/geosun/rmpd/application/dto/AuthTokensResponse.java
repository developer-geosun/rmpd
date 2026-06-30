package com.geosun.rmpd.application.dto;

public record AuthTokensResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserInfoResponse user
) {
}
