package com.geosun.rmpd.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginCommand(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
