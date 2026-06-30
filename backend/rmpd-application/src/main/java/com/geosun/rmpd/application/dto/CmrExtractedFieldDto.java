package com.geosun.rmpd.application.dto;

import java.util.List;

public record CmrExtractedFieldDto(String fieldKey, String value, double confidence) {}
