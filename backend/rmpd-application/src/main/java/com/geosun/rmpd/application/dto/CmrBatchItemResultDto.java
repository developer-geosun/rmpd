package com.geosun.rmpd.application.dto;

public record CmrBatchItemResultDto(
        Long declarationId,
        String filename,
        boolean success,
        String error,
        int extractedFieldCount) {}
