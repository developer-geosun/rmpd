package com.geosun.rmpd.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoutePointDto(
        @NotBlank @Size(max = 20) String type,
        @NotBlank @Size(max = 100) String name,
        @Size(min = 2, max = 2) String country) {}
