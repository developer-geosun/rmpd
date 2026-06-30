package com.geosun.rmpd.application.dto;

import java.util.List;

public record ValidationResultDto(boolean valid, List<String> errors) {}
