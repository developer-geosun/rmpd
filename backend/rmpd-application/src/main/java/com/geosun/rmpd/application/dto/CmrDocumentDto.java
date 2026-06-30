package com.geosun.rmpd.application.dto;

import java.util.List;

public record CmrDocumentDto(
        Long id,
        String originalFilename,
        String mimeType,
        long fileSizeBytes,
        List<CmrExtractedFieldDto> extractedFields,
        String appliedAt) {}
