package com.geosun.rmpd.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Список елементів словника")
public record DictionaryResponse(
        String type,
        List<DictionaryEntryResponse> entries
) {
}
