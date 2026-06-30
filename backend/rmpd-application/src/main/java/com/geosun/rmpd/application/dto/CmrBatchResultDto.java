package com.geosun.rmpd.application.dto;

import java.util.List;

public record CmrBatchResultDto(int total, int succeeded, List<CmrBatchItemResultDto> items) {}
