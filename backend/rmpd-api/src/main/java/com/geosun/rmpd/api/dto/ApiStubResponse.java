package com.geosun.rmpd.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Заглушка — буде реалізовано у фазі 1")
public record ApiStubResponse(
        String message
) {
    public static ApiStubResponse notImplemented(String feature) {
        return new ApiStubResponse(feature + " — not implemented yet (phase 1+)");
    }
}
