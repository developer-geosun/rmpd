package com.geosun.rmpd.application.dto;

import java.util.List;

public record DeclarationProgressDto(int completionPercent, List<String> missingFields) {}
