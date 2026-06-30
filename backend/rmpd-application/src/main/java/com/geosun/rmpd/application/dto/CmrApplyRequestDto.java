package com.geosun.rmpd.application.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CmrApplyRequestDto(@NotEmpty List<String> fieldKeys) {}
