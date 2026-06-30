package com.geosun.rmpd.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Елемент словника PUESC")
public record DictionaryEntryResponse(
        String code,
        String labelPl,
        String labelEn
) {
}
