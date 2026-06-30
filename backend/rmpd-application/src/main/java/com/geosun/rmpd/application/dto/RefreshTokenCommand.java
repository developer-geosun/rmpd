package com.geosun.rmpd.application.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenCommand(@NotBlank String refreshToken) {
}
